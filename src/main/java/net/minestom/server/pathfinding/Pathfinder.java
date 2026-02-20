package net.minestom.server.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.collections.BinaryMinimumHeap;
import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.context.PathfindingContext;
import net.minestom.server.pathfinding.data.*;
import net.minestom.server.pathfinding.movement.MovementStrategies;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import net.minestom.server.pathfinding.validation.NodeValidator;
import net.minestom.server.pathfinding.validation.ValidationStatus;
import net.minestom.server.pathfinding.validation.types.FastNodeValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Pathfinder {

    public static final Pathfinder DEFAULT_PATHFINDER = new Pathfinder(
            new PathfinderOptions.Builder()
                    .nodeValidator(new FastNodeValidator())
                    .build()
    );

    private static final double TIE_BREAKER_WEIGHT = 1e-6;

    private final PathfinderOptions options;

    public Pathfinder(@NotNull PathfinderOptions options) {
        this.options = options;
    }

    public CompletableFuture<Path> findPath(@NotNull Point start,
                                            @NotNull Point target,
                                            @NotNull MobContext mobContext,
                                            int completionRange) {
        if (options.async()) {
            return CompletableFuture.supplyAsync(() ->
                    evaluatePath(start, target, mobContext, completionRange), PathfinderScheduler.PATHING_EXECUTOR_SERVICE);
        } else {
            return CompletableFuture.completedFuture(evaluatePath(start, target, mobContext, completionRange));
        }
    }

    @NotNull
    private Path evaluatePath(@NotNull Point start,
                              @NotNull Point target,
                              @NotNull MobContext mobContext,
                              int completionRange) {
        final Node startNode = new Node(start, start, target, 0);

        // make a minimum heap priority queue and sort by the lowest F value
        final BinaryMinimumHeap openSet = new BinaryMinimumHeap(1024);
        final Long2ObjectMap<GridRegionData> visitedRegions = new Long2ObjectOpenHashMap<>();
        final Long2ObjectMap<Node> openSetNodes = new Long2ObjectOpenHashMap<>();

        final PathfindingContext pathfindingContext = new PathfindingContext(openSet, visitedRegions, openSetNodes);

        // insert the starting node
        insertNode(startNode, pathfindingContext);

        int iteration = 0;
        Node bestFallbackNode = startNode;

        while (!openSet.isEmpty() && iteration < options.maxIterations()) {
            iteration++;

            // TODO: check if find path is cancelled

            Node currentNode = extractBestNode(pathfindingContext);
            markNodeAsExpanded(currentNode, pathfindingContext);

            if (currentNode.getH() < bestFallbackNode.getH()) {
                bestFallbackNode = currentNode;
            }

            // TODO: path length limit

            // check if we have finished pathing
            if (currentNode.point().manhattanDistance(target) <= completionRange) {
                return reconstructPath(start, target, currentNode);
            }

            processNeighbors(start, target, currentNode, pathfindingContext, mobContext);
        }

        // TODO: fail or best effort path
        return new Path(Path.State.FAILED, Collections.emptyList(), start, target);
    }

    @NotNull
    private Path reconstructPath(@NotNull Point start,
                                 @NotNull Point target,
                                 @NotNull Node endNode) {
        if (endNode.getParentNode() == null && endNode.depth() == 0) {
            return new Path(Path.State.FOUND, Collections.singletonList(new PathPoint(endNode.point(), endNode.getType())), start, target);
        }

        final List<PathPoint> pathPoints = new ArrayList<>();
        Node currentNode = endNode;
        while (currentNode != null) {
            pathPoints.add(new PathPoint(currentNode.point(), currentNode.getType()));
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

    private void insertNode(@NotNull Node startNode,
                            @NotNull PathfindingContext pathfindingContext) {
        insertNode(null, startNode, pathfindingContext);
    }

    private void insertNode(@Nullable Node parentNode,
                            @NotNull Node newNode,
                            @NotNull PathfindingContext pathfindingContext) {
        if (parentNode != null) {
            newNode.setParentNode(parentNode);

            // TODO: more advanced cost processing
            final double g = parentNode.getG() + 1.0D;
            newNode.setG(g);
        }

        final double heapKey = calculateHeapKey(newNode, newNode.getF());
        final long packedPoint = RegionKey.pack(newNode.point());
        pathfindingContext.openSet().insertOrUpdate(packedPoint, heapKey);
        pathfindingContext.openSetNodes().put(packedPoint, newNode);
    }

    private Node extractBestNode(@NotNull PathfindingContext pathfindingContext) {
        final long packedPoint = pathfindingContext.openSet().extractMin();
        final Node node = pathfindingContext.openSetNodes().get(packedPoint);
        pathfindingContext.openSetNodes().remove(packedPoint);

        return node;
    }

    private void markNodeAsExpanded(@NotNull Node node,
                                    @NotNull PathfindingContext pathfindingContext) {
        final Point point = node.point();

        final long packedPoint = RegionKey.pack(point);
        pathfindingContext.openSetNodes().remove(packedPoint);

        // TODO: reopen closed nodes

        final GridRegionData regionData = getOrCreateRegionData(point, pathfindingContext);
        regionData.getBloomFilter().put(point);
        regionData.getRegionalExaminedPositions().add(packedPoint);
    }

    private void updateExistingNode(@NotNull Node existingNode,
                                    long packedPoint,
                                    @NotNull Node currentNode,
                                    @NotNull PathfindingContext pathfindingContext) {
        final double newG = currentNode.getG() + 1.0D;
        final double tol = Math.ulp(Math.max(Math.abs(newG), Math.abs(existingNode.getG())));
        if (newG + tol >= existingNode.getG()) {
            return;
        }

        // TODO: check if it's a valid node

        existingNode.setParentNode(currentNode);
        existingNode.setG(newG);

        final double newKey = calculateHeapKey(existingNode, existingNode.getF());
        final double oldKey = pathfindingContext.openSet().getCost(packedPoint);

        // We only call the heap once the key actually decreased
        if (newKey + Math.ulp(newKey) < oldKey) {
            pathfindingContext.openSet().insertOrUpdate(packedPoint, newKey);
        } else if (Math.abs(newKey - oldKey) <= Math.ulp(newKey)) {
            /*
             * Sometimes a tiny nudging helps to maintain consistency,
             * but usually insertOrUpdate catches that.
             *
             * Since our heap strictly checks <, we can force it here
             */
            pathfindingContext.openSet().insertOrUpdate(packedPoint, oldKey - Math.ulp(oldKey));
        }
    }

    private GridRegionData getOrCreateRegionData(@NotNull Point point,
                                                 @NotNull PathfindingContext pathfindingContext) {
        final int cellSize = 12;

        final int rX = Math.floorDiv(point.blockX(), cellSize);
        final int rY = Math.floorDiv(point.blockY(), cellSize);
        final int rZ = Math.floorDiv(point.blockZ(), cellSize);

        final long regionKey = RegionKey.pack(rX, rY, rZ);

        return pathfindingContext.visitedRegions().computeIfAbsent(regionKey,
                (long k) -> new GridRegionData(options));
    }

    private void processNeighbors(@NotNull Point start,
                                  @NotNull Point target,
                                  @NotNull Node currentNode,
                                  @NotNull PathfindingContext pathfindingContext,
                                  @NotNull MobContext mobContext) {
        outer:
        for (Vec offset : MovementStrategies.DIAGONAL_MOVEMENT_STRATEGY) {
            final Point neighborPoint = currentNode.point().add(offset);
            final long packedPoint = RegionKey.pack(neighborPoint);

            // chck if the neighbor is in the open set
            if (pathfindingContext.openSet().contains(packedPoint)) {
                final Node existingNode = pathfindingContext.openSetNodes().get(packedPoint);
                updateExistingNode(existingNode, packedPoint, currentNode, pathfindingContext);
                continue;
            }

            // check if the neighbor is in the closed set
            final GridRegionData regionData = getOrCreateRegionData(neighborPoint, pathfindingContext);
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
                final ValidationStatus validationStatus = nodeValidator.checkValidity(currentNode, neighborNode, mobContext);

                // if the node isn't valid at all, we'll just skip it and continue to the next one
                if (!validationStatus.valid())
                    continue outer;

                // if the node is valid, and we have an updated node then we must insert it into the open set since it means we either jumped or fell
                final Node updatedNode = validationStatus.updatedNode();
                if (updatedNode != null) {
                    insertNode(currentNode, updatedNode, pathfindingContext);

                    // TODO: for debug, remove me
                    mobContext.instance().setBlock(updatedNode.point().sub(0, 1, 0), Block.RED_WOOL);

                    continue outer;
                }
            }

            insertNode(currentNode, neighborNode, pathfindingContext);

            // TODO: for debug, remove me
            mobContext.instance().setBlock(neighborPoint.sub(0, 1, 0), Block.GREEN_WOOL);
        }
    }
}