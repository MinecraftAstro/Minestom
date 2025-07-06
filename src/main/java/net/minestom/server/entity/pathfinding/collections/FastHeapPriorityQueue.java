package net.minestom.server.entity.pathfinding.collections;

import net.minestom.server.entity.pathfinding.nodes.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FastHeapPriorityQueue {

    private final int capacity;
    private final Node[] heap;

    private int size;

    public FastHeapPriorityQueue(int capacity) {
        this.capacity = capacity;
        this.heap = new Node[capacity];
        this.size = 0;
    }

    public void enqueue(@NotNull Node node) {
        if (size == capacity)
            return;

        heap[size++] = node;
        upHeap();
    }

    private void upHeap() {
        int index = size - 1;
        final Node element = heap[index];
        while (index != 0) {
            final int parent = (index - 1) >>> 1;
            final Node parentNode = heap[parent];
            if (element.f() >= parentNode.f())
                break;

            heap[index] = parentNode;
            index = parent;
        }

        heap[index] = element;
    }

    @Nullable
    public Node dequeue() {
        if (size == 0)
            return null;

        final Node result = heap[0];
        heap[0] = heap[--size];
        heap[size] = null;
        if (size != 0)
            downHeap();

        return result;
    }

    private void downHeap() {
        int index = 0;
        final Node element = heap[index];
        final double elementF = element.f();
        final int halfSize = size >>> 1; // Only need to check up to half the size

        while (index < halfSize) {
            int childIndex = (index << 1) + 1; // Left child
            Node childNode = heap[childIndex];
            final int rightChildIndex = childIndex + 1;

            // Find the smaller child
            if (rightChildIndex < size && heap[rightChildIndex].f() < childNode.f()) {
                childIndex = rightChildIndex;
                childNode = heap[childIndex];
            }

            // If element is smaller than or equal to smallest child, we're done
            if (elementF <= childNode.f()) {
                break;
            }

            heap[index] = childNode;
            index = childIndex;
        }

        heap[index] = element;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}