package net.minestom.server.pathfinding;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import net.minestom.server.pathfinding.data.Node;

public class Pathfinder {

    public void generatePath() {
        // TODO: node comparator inside this or elsewhere
        // TODO: default size to avoid priority queue resizing
        final PriorityQueue<Node> nodes = new ObjectHeapPriorityQueue<>();

        while (!nodes.isEmpty()) {
            Node currentNode = nodes.dequeue();
        }
    }
}