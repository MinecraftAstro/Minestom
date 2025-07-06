package net.minestom.server.entity.pathfinding.path.generator;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.pathfinding.collections.FastHeapPriorityQueue;
import net.minestom.server.entity.pathfinding.nodes.Node;
import net.minestom.server.entity.pathfinding.nodes.generator.NodeGenerator;
import net.minestom.server.entity.pathfinding.path.Path;
import net.minestom.server.entity.pathfinding.path.PathWalker;
import net.minestom.server.instance.BlockBatch;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public final class PathGenerator {

    private static final Comparator<Node> NODE_COMPARATOR = (node1, node2) -> (int) (((node1.f()) - (node2.f())) * 1000);
    //private static final Comparator<Node> NODE_COMPARATOR = Comparator.comparingDouble(Node::f).reversed();

    @NotNull
    public static CompletableFuture<Path> generatePath(@NotNull PathWalker pathWalker,
                                                       @NotNull NodeGenerator nodeGenerator,
                                                       @NotNull Point startPoint,
                                                       @NotNull Point endPoint) {
        final CompletableFuture<Path> future = new CompletableFuture<>();

        ForkJoinPool.commonPool().execute(() -> {
            final int minChunkX = Math.min(startPoint.chunkX(), endPoint.chunkX());
            final int minChunkZ = Math.min(startPoint.chunkZ(), endPoint.chunkZ());
            final int maxChunkX = Math.max(startPoint.chunkX(), endPoint.chunkX());
            final int maxChunkZ = Math.max(startPoint.chunkZ(), endPoint.chunkZ());

            final Point point1 = new Vec(minChunkX << 4, -64, minChunkZ << 4);
            final Point point2 = new Vec((maxChunkX << 4) + 15, 319, (maxChunkZ << 4) + 15);

            final BlockBatch blockBatch = pathWalker.getInstance().getBlockBatch(Vec.ZERO, point1, point2);

            // a list containing the nodes which have not been explored yet
            // this will be sorted through the node's F cost, which will leave the lowest F cost node at the top
            // 16,006ms
            final PriorityQueue<Node> openNodes = new ObjectHeapPriorityQueue<>(3500, NODE_COMPARATOR);
            //final FastHeapPriorityQueue openNodes = new FastHeapPriorityQueue(4500);

            // our starting node will represent the start of the path
            // we will start our A* search from this node
            final Node startNode = new Node(Node.Type.START, 0, startPoint.distance(endPoint), startPoint, null);
            openNodes.enqueue(startNode);

            // a list containing the nodes which have been explored
            // TODO: test a ObjectOpenBigHashSet vs an ObjectOpenHashSet
            final Set<Node> closedNodes = new ObjectOpenHashSet<>();

            while (!openNodes.isEmpty()) {
                // 49,181ms
                final Node currentNode = openNodes.dequeue();

                // check if we've already processed this node
                if (closedNodes.contains(currentNode))
                    continue;

                // mark this node as closed, which means it has been processed
                closedNodes.add(currentNode);

                if(currentNode.point().distance(endPoint) <= 1.0D) {
                    openNodes.enqueue(currentNode);
                    //System.out.println("Found end point");
                    break;
                }

                // generate a collection of traversable nodes around the current node
                // 49920ms
                final Collection<Node> neighborNodes = nodeGenerator.generateNodes(pathWalker, blockBatch, currentNode, endPoint);
                for (Node neighborNode : neighborNodes) {
                    // skip this node if it's already been processed
                    if (closedNodes.contains(neighborNode))
                        continue;

                    // 41,369 ms
                    openNodes.enqueue(neighborNode);
                }
            }

            final Path path = new Path();

            Node currentNode = openNodes.dequeue();
            if (currentNode == null) {
                future.complete(null);
                return;
            }

            while (currentNode.parentNode() != null) {
                path.getNodes().add(currentNode);
                currentNode = currentNode.parentNode();
            }

            Collections.reverse(path.getNodes());

            final Node endNode = new Node(Node.Type.END, 0, 0, endPoint, null);
            path.getNodes().add(endNode);

            //System.out.println(Arrays.toString(path.getNodes().toArray()));

            future.complete(path);
        });

        return future;
    }

    public static void drawPath(Player player,
                                Path path) {
        if (path == null) return;

        final List<SendablePacket> particles = new ArrayList<>();
        for (Node node : path.getNodes()) {
            final ParticlePacket particlePacket = new ParticlePacket(Particle.COMPOSTER, node.point().blockX(), node.point().blockY() + 0.5, node.point().blockZ(), 0, 0, 0, 0, 1);
            particles.add(particlePacket);
        }

        player.sendPackets(particles);
    }

    private static Path buildPath() {
        return null;
    }

//    public final class PathGenerator {
//
//        // generates a path with a collection of nodes to traverse (if a path is possible)
//        public static Path generatePath(@NotNull LivingEntity entity,
//                                        @NotNull Point startPoint,
//                                        @NotNull Point endPoint,
//                                        double maxSearchDistance,
//                                        double completionDistance,
//                                        @Nullable Runnable onCompletion,
//                                        @NotNull NodeGenerator nodeGenerator) {
//            final Path path = new Path(maxSearchDistance, completionDistance, onCompletion);
//            generatePathNodes(path, entity, startPoint, endPoint, maxSearchDistance, completionDistance, nodeGenerator);
//            return path;
//        }
//
//        private static void generatePathNodes(@NotNull Path path,
//                                              @NotNull LivingEntity entity,
//                                              @NotNull Point startPoint,
//                                              @NotNull Point destinationPoint,
//                                              double maxSearchDistance,
//                                              double completionDistance,
//                                              //double pathVariance,
//                                              @NotNull NodeGenerator nodeGenerator) {
//            // the max search distance represents the distance from the start of the pathfinding
//            // the max closed nodes size is to prevent the closedNodesList from becoming too large
//            int maxClosedNodesSize = (int) Math.floor(maxSearchDistance * 10);
//
//            // we want to make sure people do set the completion distance to something extremely low which will be computationally expensive
//            completionDistance = Math.max(0.8d, completionDistance);
//
//            // a list containing the nodes which have not been explored yet
//            // this will be sorted through the node's F cost, which will leave the lowest F cost node at the top
//            final PriorityQueue<Node> openNodesList = new ObjectHeapPriorityQueue<>();
//
//            // our starting node will represent the start of the path
//            final Node startNode = new Node(null, 0, startPoint.distance(destinationPoint), startPoint, Node.Type.WALK);
//            openNodesList.enqueue(startNode);
//
//            // a list containing the nodes which have been explored
//            final Set<Node> closedNodesList = new ObjectOpenHashBigSet<>();
//
//            // these allow us to implement a fallback mechanism that allows the entity to move towards the destination even if their is not a complete path
//            double closestDistance = Double.MAX_VALUE;
//            Node closestNode = null;
//
//            // make sure that we have open nodes to explore and that we haven't reached our capacity on the closed nodes list
//            while (!openNodesList.isEmpty() && closedNodesList.size() < maxClosedNodesSize) {
//                // check if we need to terminate the path generation, this could be because the user wants to end path generation, the entity died, etc...
//                if (path.getState() == Path.State.TERMINATED)
//                    return;
//
//                final Node currentNode = openNodesList.dequeue();
//
//                // check if we've already processed this node
//                if (closedNodesList.contains(currentNode))
//                    continue;
//
//                // mark this node as closed, which means it has been processed
//                closedNodesList.add(currentNode);
//
//                // we can't pathfind in chunks that aren't loaded, not only is their no point in doing this, but it would also complicate things
//                final Chunk chunk = entity.getInstance().getChunkAt(currentNode.x(), currentNode.z());
//                if (!ChunkUtils.isLoaded(chunk)) continue;
//
//                // TODO: add path variance checks?
//
//                // check if we have exceeded our max search distance from the start point
//                if (!currentNode.getPoint().withinDistanceSquared(startPoint, maxSearchDistance)) continue;
//
//                // check if the current node is within our completion distance to the destination point
//                // if it is, we have generated a path to the destination point within the completion distance and we can stop exploring
//                if (currentNode.getPoint().withinDistanceSquared(destinationPoint, completionDistance)) {
//                    buildPath(path, currentNode, destinationPoint, completionDistance, false);
//                    return;
//                }
//
//                // we will use the closest node as backup in-case we haven't reached the destination point at the end of iteration
//                if (currentNode.h() < closestDistance) {
//                    closestDistance = currentNode.h();
//                    closestNode = currentNode;
//                }
//
//                // generate a list of traversable nodes around the current node
//                final List<Node> traversableNodes = nodeGenerator.generateNodes(currentNode, destinationPoint, closedNodesList);
//                for (Node neighborNode : traversableNodes) {
//                    // skip this node if it's already been processed
//                    if (closedNodesList.contains(neighborNode))
//                        continue;
//
//                    // skip this node if it's outside the max search distance
//                    if (!neighborNode.getPoint().withinDistanceSquared(startPoint, maxSearchDistance))
//                        continue;
//
//                    openNodesList.enqueue(neighborNode);
//                }
//            }
//
//            // check if we need to re-path, which means we have hit the max nodes but still have open nodes
//            boolean needsRepathing = closedNodesList.size() >= maxClosedNodesSize && !openNodesList.isEmpty();
//
//            // no path found to the destination, use the closest node if possible
//            if (closestNode != null) {
//                buildPath(path, closestNode, destinationPoint, completionDistance, needsRepathing);
//            } else {
//                path.setState(Path.State.INVALID);
//            }
//        }
//
//        private static void buildPath(@NotNull Path path,
//                                      @NotNull Node lastNode,
//                                      @NotNull Point destinationPoint,
//                                      double completionDistance,
//                                      boolean needsRepathing) {
//            // build the path with our traversable nodes
//            Node currentNode = lastNode;
//            while (currentNode.getParentNode() != null) {
//                path.getNodes().add(currentNode);
//                currentNode = currentNode.getParentNode();
//            }
//
//            // make sure that we have nodes in our path or else it is an invalid path
//            if (path.getNodes().isEmpty()) {
//                path.setState(Path.State.INVALID);
//                return;
//            }
//
//            // reverse the list of nodes so that we go from start node -> end node
//            Collections.reverse(path.getNodes());
//
//            // if our end node is not within the completion distance, we'll mark this path as partially computed since it will attempt to make its way to the destination as much as possible
//            final Node endNode = path.getNodes().getLast();
//            if (endNode.getPoint().withinDistanceSquared(destinationPoint, completionDistance)) {
//                // we will reach the destination point without any problems, this path has been computed successfully
//                path.setState(Path.State.COMPUTED);
//                //System.out.println("Path computed successfully");
//            } else {
//                if (needsRepathing) {
//                    final Node repathNode = new Node(endNode, 0, 0, Pos.ZERO, Node.Type.REPATH);
//                    path.getNodes().add(repathNode);
//                    path.setState(Path.State.PARTIALLY_COMPUTED);
//                    //System.out.println("Path requires repathing");
//                } else {
//                    path.setState(Path.State.PARTIALLY_COMPUTED);
//                    //System.out.println("Path partially computed");
//                }
//            }
//
//        /*System.out.println("Nodes");
//        for (Node node : path.getNodes()) {
//            System.out.println(node);
//        }
//        System.out.println();*/
//        }
//    }
}