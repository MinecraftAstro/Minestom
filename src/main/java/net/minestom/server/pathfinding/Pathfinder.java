package net.minestom.server.pathfinding;

import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class Pathfinder {

    // TODO: test the performance of this vs a built-in Node Comparable with a custom heap priority queue
    private static final Comparator<Node> NODE_COMPARATOR = new Comparator<Node>() {
        @Override
        public int compare(Node o1, Node o2) {
            return 0;
        }
    };

    public void generatePath(@NotNull Point startPoint,
                             @NotNull Point endPoint,
                             @NotNull Block.Getter context,
                             @NotNull BoundingBox boundingBox,
                             @NotNull PathfinderOptions options) {
        // TODO: node comparator inside this or elsewhere
        // TODO: default size to avoid priority queue resizing
        // TODO: overestimate the heuristic with some dynamic function

        // make a minimum heap priority queue and sort by the lowest F value
        final ObjectHeapPriorityQueue<Node> nodes = new ObjectHeapPriorityQueue<>(NODE_COMPARATOR);

        // the initial node represents the node at the starting point
        final Node initialNode = new Node(startPoint, 0.0f, startPoint.chebyshevDistance(endPoint));
        nodes.enqueue(initialNode);

        while (!nodes.isEmpty()) {
            // get the node with the smallest F cost
            final Node currentNode = nodes.dequeue();

            // check if we are within the completion range from the end point for this path
            if (withinChebyshevDistance(currentNode, endPoint, options.completionRange())) {
                nodes.enqueue(currentNode);
                break;
            }

            final Node[] validNeighborNodes = options.nodeGenerator().getValidNeighbors(currentNode, context, boundingBox);
            for (Node neighborNode : validNeighborNodes) {
                final double distance = distanceTo(currentNode, neighborNode);
                final double tentativeG = currentNode.getG() + distance;
            }
        }
    }

    private static double distanceTo(@NotNull Node currentNode,
                                     @NotNull Node targetNode) {
        final double dx = Math.abs(targetNode.x() - currentNode.x());
        final double dy = Math.abs(targetNode.y() - currentNode.y());
        final double dz = Math.abs(targetNode.z() - currentNode.z());
        return Math.max(dx, Math.max(dy, dz));
    }

    private static boolean withinEuclideanDistance(@NotNull Node currentNode,
                                                   @NotNull Point targetPoint,
                                                   double distance) {
        final double dx = MathUtils.square(targetPoint.x() - currentNode.x());
        final double dy = MathUtils.square(targetPoint.y() - currentNode.y());
        final double dz = MathUtils.square(targetPoint.z() - currentNode.z());
        return Math.sqrt(dx + dy + dz) <= distance;
    }

    private static boolean withinManhattanDistance(@NotNull Node currentNode,
                                                   @NotNull Point targetPoint,
                                                   double distance) {
        final double dx = Math.abs(targetPoint.x() - currentNode.x());
        final double dy = Math.abs(targetPoint.y() - currentNode.y());
        final double dz = Math.abs(targetPoint.z() - currentNode.z());
        return (dx + dy + dz) <= distance;
    }

    private static boolean withinChebyshevDistance(@NotNull Node currentNode,
                                                   @NotNull Point targetPoint,
                                                   double distance) {
        final double dx = Math.abs(targetPoint.x() - currentNode.x());
        final double dy = Math.abs(targetPoint.y() - currentNode.y());
        final double dz = Math.abs(targetPoint.z() - currentNode.z());
        return (Math.max(dx, Math.max(dy, dz))) <= distance;
    }
}