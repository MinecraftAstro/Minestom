package net.minestom.server.pathfinding.data;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

public final class Node {

    private final int x;
    private final int y;
    private final int z;

    private double g;
    private double h;
    private double f;

    private boolean closed;

    private final int hash;

    public Node(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hash = createHash(x, y, z);
    }

    public Node(int x,
                int y,
                int z,
                double g,
                double h) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.g = g;
        this.h = h;
        this.f = g + h;
        this.hash = createHash(x, y, z);
    }

    public Node(@NotNull Point point,
                double g,
                double h) {
        this(point.blockX(), point.blockY(), point.blockZ(), g, h);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public double getG() {
        return g;
    }

    public double getH() {
        return h;
    }

    public double getF() {
        return f;
    }

    public void setG(double g) {
        this.g = g;
        this.f = g + h;
    }

    public void setH(double h) {
        this.h = h;
        this.f = g + h;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    private int createHash(int x, int y, int z) {
        return y & 0xFF | (x & 32767) << 8 | (z & 32767) << 24 | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? 32768 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;

        Node node = (Node) obj;
        return x == node.x && y == node.y && z == node.z && hash == node.hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    // useful for debugging
    @Override
    public String toString() {
        return "Node{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", g=" + g +
                ", h=" + h +
                ", f=" + f +
                ", hash=" + hash +
                '}';
    }

    public enum Type {

        /**
         * Indicates that this
         */
        OPEN
    }
}