package net.minestom.server.pathfinding.data;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Node implements Comparable<Node> {

    private final Point point;

    private double g;
    private double h;

    private Node parentNode;
    private final int depth;

    private final Point start;
    private final Point target;

    private Block groundBlock;

    private Type type;

    public Node(@NotNull Point point,
                @NotNull Point start,
                @NotNull Point target,
                int depth) {
        this.point = point;
        this.depth = depth;
        this.g = 0;
        this.h = point.manhattanDistance(target);
        this.start = start;
        this.target = target;
        this.type = Type.EMPTY;
    }

    @NotNull
    public Point point() {
        return point;
    }

    public double getG() {
        return g;
    }

    public double getH() {
        return h;
    }

    public double getF() {
        // add a small weight to increase performance with a trade-off for path optimality
        return g + 1.5 * h;
    }

    public void setG(double g) {
        this.g = g;
    }

    public void setH(double h) {
        this.h = h;
    }

    @Nullable
    public Node getParentNode() {
        return parentNode;
    }

    public void setParentNode(@Nullable Node parentNode) {
        this.parentNode = parentNode;
    }

    public int depth() {
        return depth;
    }

    @NotNull
    public Point start() {
        return start;
    }

    @NotNull
    public Point target() {
        return target;
    }

    @NotNull
    public Block getGroundBlock() {
        return groundBlock == null ? Block.AIR : groundBlock;
    }

    public void setGroundBlock(@Nullable Block groundBlock) {
        this.groundBlock = groundBlock;
    }

    public void setType(@NotNull Type type) {
        this.type = type;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Override
    public int compareTo(@NotNull Node other) {
        // First compare by F-cost (G-cost + H-cost)
        int fCostComparison = Double.compare(this.getF(), other.getF());
        if (fCostComparison != 0) {
            return fCostComparison;
        }

        // If F-costs are equal, compare by heuristic value
        int heuristicComparison = Double.compare(this.getH(), other.getH());
        if (heuristicComparison != 0) {
            return heuristicComparison;
        }

        // If heuristics are equal, compare by depth
        return Integer.compare(this.depth, other.depth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Node that = (Node) obj;
        return point.blockX() == that.point.blockX()
                && point.blockY() == that.point.blockY()
                && point.blockZ() == that.point.blockZ();
    }

    @Override
    public int hashCode() {
        int x = point.blockX();
        int y = point.blockY();
        int z = point.blockZ();
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "Node{" +
                "groundBlock=" + getGroundBlock() +
                ", g=" + g +
                ", h=" + h +
                ", f=" + getF() +
                '}';
    }

    public enum Type {

        EMPTY,

        FALL,

        STEP,

        JUMP,

        // move through water
        SWIM,

        // float on water, this happens if the path ends up with the end point being a liquid block
        FLOAT
    }
}