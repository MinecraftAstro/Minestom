package net.minestom.server.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.collections.BinaryMinimumHeap;
import net.minestom.server.pathfinding.data.GridRegionData;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.pathfinding.data.RegionKey;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import net.minestom.server.pathfinding.validation.NodeValidator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Pathfinder {

    private static final ExecutorService PATHING_EXECUTOR_SERVICE =
            Executors.newWorkStealingPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Pathfinder::shutdownExecutor));
    }

    private static final double TIE_BREAKER_WEIGHT = 1e-6;

    private static final Iterable<Vec> BASIC_MOVEMENT = Arrays.asList(
            new Vec(-1, 0, 0),
            new Vec(0, 0, -1),
            new Vec(0, 0, 1),
            new Vec(1, 0, 0)
    );

    private static final Iterable<Vec> DIAGONAL_MOVEMENT = Arrays.asList(
            new Vec(-1, 0, -1),
            new Vec(-1, 0, 0),
            new Vec(-1, 0, 1),
            new Vec(0, 0, -1),
            new Vec(0, 0, 1),
            new Vec(1, 0, -1),
            new Vec(1, 0, 0),
            new Vec(1, 0, 1)
    );

    private final PathfinderOptions options;

    public Pathfinder(@NotNull PathfinderOptions options) {
        this.options = options;
    }

    public CompletableFuture<Path> findPath(@NotNull Point start,
                                            @NotNull Point target,
                                            @NotNull Instance instance,
                                            @NotNull BoundingBox boundingBox) {
        if(options.async()) {
            return CompletableFuture.supplyAsync(() ->
                    evaluatePath(start, target, instance, boundingBox), PATHING_EXECUTOR_SERVICE);
        } else {
            return CompletableFuture.completedFuture(evaluatePath(start, target, instance, boundingBox));
        }
    }

    @NotNull
    private Path evaluatePath(@NotNull Point start,
                                      @NotNull Point target,
                                      @NotNull Instance instance,
                                      @NotNull BoundingBox boundingBox) {
        final Node startNode = new Node(start, start, target, 0);

        // make a minimum heap priority queue and sort by the lowest F value
        final BinaryMinimumHeap openSet = new BinaryMinimumHeap(1024);
        final Long2ObjectMap<GridRegionData> visitedRegions = new Long2ObjectOpenHashMap<>();
        final Long2ObjectMap<Node> openSetNodes = new Long2ObjectOpenHashMap<>();

        insertStartNode(startNode, openSet, openSetNodes);

        int currentDepth = 0;
        Node bestFallbackNode = startNode;

        while (!openSet.isEmpty() && currentDepth < options.maxIterations()) {
            currentDepth++;

            // TODO: check if find path is cancelled

            Node currentNode = extractBestNode(openSet, openSetNodes);
            markNodeAsExpanded(currentNode, visitedRegions, openSetNodes);

            if (currentNode.getH() < bestFallbackNode.getH()) {
                bestFallbackNode = currentNode;
            }

            // TODO: path length limit

            // check if we have finished pathing
            if (currentNode.point().manhattanDistance(target) <= options.completionRange()) {
                return reconstructPath(start, target, currentNode);
            }

            processNeighbors(start, target, currentNode, openSet, openSetNodes, visitedRegions, boundingBox, instance);
        }

        // TODO: fail or best effort path
        return new Path(Path.State.FAILED, Collections.emptyList(), start, target);
    }

    @NotNull
    private Path reconstructPath(@NotNull Point start,
                                 @NotNull Point target,
                                 @NotNull Node endNode) {
        if (endNode.getParentNode() == null && endNode.depth() == 0) {
            return new Path(Path.State.FOUND, Collections.singletonList(endNode.point()), start, target);
        }

        final List<Point> pathPoints = new ArrayList<>();
        Node currentNode = endNode;
        while (currentNode != null) {
            pathPoints.add(currentNode.point());
            currentNode = currentNode.getParentNode();
        }

        Collections.reverse(pathPoints);
        return new Path(Path.State.FOUND, pathPoints, start, target);
    }

    private double calculateHeapKey(Node neighbor, double f) {
        double heuristic = neighbor.getH();
        double tieBreaker = TIE_BREAKER_WEIGHT * (heuristic / (Math.abs(f) + 1));
        double heapKey = f - tieBreaker;

        if (Double.isNaN(heapKey) || Double.isInfinite(heapKey)) {
            heapKey = f;
        }

        return heapKey;
    }

    private void insertStartNode(@NotNull Node startNode,
                                 @NotNull BinaryMinimumHeap openSet,
                                 Long2ObjectMap<Node> openSetNodes) {
        // TODO: do we need a try-catch here?
        double startKey;
        try {
            startKey = calculateHeapKey(startNode, startNode.getF());
        } catch (Throwable ignored) {
            startKey = startNode.getF();
        }

        final long packedPoint = RegionKey.pack(startNode.point());
        openSet.insertOrUpdate(packedPoint, startKey);
        openSetNodes.put(packedPoint, startNode);
    }

    private Node extractBestNode(@NotNull BinaryMinimumHeap openSet,
                                 Long2ObjectMap<Node> openSetNodes) {
        final long packedPoint = openSet.extractMin();
        final Node node = openSetNodes.get(packedPoint);
        openSetNodes.remove(packedPoint);

        return node;
    }

    private void markNodeAsExpanded(@NotNull Node node,
                                    Long2ObjectMap<GridRegionData> visitedRegions,
                                    Long2ObjectMap<Node> openSetNodes) {
        final Point point = node.point();

        final long packedPoint = RegionKey.pack(point);
        openSetNodes.remove(packedPoint);

        // TODO: reopen closed nodes

        final GridRegionData regionData = getOrCreateRegionData(point, visitedRegions);
        regionData.getBloomFilter().put(point);
        regionData.getRegionalExaminedPositions().add(packedPoint);
    }

    private void updateExistingNode(@NotNull Node existingNode,
                                    long packedPoint,
                                    @NotNull Node currentNode,
                                    @NotNull BinaryMinimumHeap openSet) {
        final double newG = currentNode.getG() + 1.0D;
        final double tol = Math.ulp(Math.max(Math.abs(newG), Math.abs(existingNode.getG())));
        if (newG + tol >= existingNode.getG()) {
            return;
        }

        // TODO: check if it's a valid node

        existingNode.setParentNode(currentNode);
        existingNode.setG(newG);

        final double newKey = calculateHeapKey(existingNode, existingNode.getF());
        final double oldKey = openSet.getCost(packedPoint);

        // We only call the heap once the key actually decreased
        if (newKey + Math.ulp(newKey) < oldKey) {
            openSet.insertOrUpdate(packedPoint, newKey);
        } else if (Math.abs(newKey - oldKey) <= Math.ulp(newKey)) {
            /*
             * Sometimes a tiny nudging helps to maintain consistency,
             * but usually insertOrUpdate catches that.
             *
             * Since our heap strictly checks <, we can force it here
             */
            openSet.insertOrUpdate(packedPoint, oldKey - Math.ulp(oldKey));
        }
    }

    private GridRegionData getOrCreateRegionData(@NotNull Point point,
                                                 Long2ObjectMap<GridRegionData> visitedRegions) {
        final int cellSize = 12;

        final int rX = Math.floorDiv(point.blockX(), cellSize);
        final int rY = Math.floorDiv(point.blockY(), cellSize);
        final int rZ = Math.floorDiv(point.blockZ(), cellSize);

        final long regionKey = RegionKey.pack(rX, rY, rZ);

        return visitedRegions.computeIfAbsent(regionKey,
                (long k) -> new GridRegionData(options));
    }

    private void processNeighbors(@NotNull Point start,
                                  @NotNull Point target,
                                  @NotNull Node currentNode,
                                  @NotNull BinaryMinimumHeap openSet,
                                  Long2ObjectMap<Node> openSetNodes,
                                  Long2ObjectMap<GridRegionData> visitedRegions,
                                  BoundingBox boundingBox,
                                  @NotNull Instance instance) {
        outer:
        for (Vec offset : BASIC_MOVEMENT) {
            final Point neighborPoint = currentNode.point().add(offset);
            final long packedPoint = RegionKey.pack(neighborPoint);

            // chck if the neighbor is in the open set
            if (openSet.contains(packedPoint)) {
                final Node existingNode = openSetNodes.get(packedPoint);
                updateExistingNode(existingNode, packedPoint, currentNode, openSet);
                continue;
            }

            // check if the neighbor is in the closed set
            final GridRegionData regionData = getOrCreateRegionData(neighborPoint, visitedRegions);
            if (regionData.getBloomFilter().mightContain(neighborPoint)
                    && regionData.getRegionalExaminedPositions().contains(packedPoint)) {
                // TODO: reopen node
                continue;
            }

            // process a new node
            final Node neighborNode = new Node(neighborPoint, start, target, currentNode.depth() + 1);
            neighborNode.setParentNode(currentNode);

            // check if the step from the current node to the neighbor node is valid
            for (NodeValidator nodeValidator : options.nodeValidators()) {
                if (!nodeValidator.isValid(currentNode, neighborNode, instance, boundingBox)) {
                    continue outer;
                }
            }

            // TODO: more advanced cost processing
            final double g = currentNode.getG() + 1.0D;
            neighborNode.setG(g);

            final double heapKey = calculateHeapKey(neighborNode, neighborNode.getF());
            openSet.insertOrUpdate(packedPoint, heapKey);
            openSetNodes.put(packedPoint, neighborNode);

            instance.setBlock(neighborPoint.sub(0, 1, 0), Block.GREEN_WOOL);
        }
    }

    private static void shutdownExecutor() {
        PATHING_EXECUTOR_SERVICE.shutdown();
        try {
            if (!PATHING_EXECUTOR_SERVICE.awaitTermination(5, TimeUnit.SECONDS)) {
                PATHING_EXECUTOR_SERVICE.shutdownNow();
            }
        } catch (InterruptedException e) {
            PATHING_EXECUTOR_SERVICE.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}