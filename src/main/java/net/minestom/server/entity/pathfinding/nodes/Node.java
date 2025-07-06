package net.minestom.server.entity.pathfinding.nodes;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Node {

    private final Type type;

    private final double g;
    private final double h;
    private final double f;

    private final Point point;

    private final Node parentNode;

    private final int hashCode;

    public Node(@NotNull Type type,
                double g,
                double h,
                @NotNull Point point,
                @Nullable Node parentNode) {
        this.type = type;
        this.g = g;
        this.h = h;
        this.f = g + h;
        this.point = point;
        this.parentNode = parentNode;
        this.hashCode = (point.blockX() * 73856093) ^ (point.blockY() * 19349663) ^ (point.blockZ() * 83492791) ^ type.hashCode();
    }

    @NotNull
    public Type type() {
        return type;
    }

    public double g() {
        return g;
    }

    public double h() {
        return h;
    }

    public double f() {
        return f;
    }

    @NotNull
    public Point point() {
        return point;
    }

    @Nullable
    public Node parentNode() {
        return parentNode;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        final Node node = (Node) object;
        return hashCode == node.hashCode;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /*@Override
    public int compareTo(@NotNull Node that) {
        return Double.compare(this.f, that.f());
    }*/

    @Override
    public String toString() {
        return "Node{" +
                "type=" + type +
                ", point=" + point +
                '}';
    }

    public enum Type {

        START,

        WALK,

        STEP,

        JUMP,

        FALL,

        END
    }
}