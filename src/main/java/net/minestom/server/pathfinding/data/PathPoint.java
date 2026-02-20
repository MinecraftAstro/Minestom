package net.minestom.server.pathfinding.data;

import net.minestom.server.coordinate.Point;

public final class PathPoint {

    private final Point point;
    private final Node.Type type;

    public PathPoint(Point point, Node.Type type) {
        this.point = point;
        this.type = type;
    }

    public Point point() {
        return point;
    }

    public Node.Type type() {
        return type;
    }
}