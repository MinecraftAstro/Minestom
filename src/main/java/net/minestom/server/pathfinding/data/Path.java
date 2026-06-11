package net.minestom.server.pathfinding.data;

import it.unimi.dsi.fastutil.objects.ObjectIterables;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Path {

    private final State state;

    private final List<Point> points;
    private final Point start;
    private final Point end;

    private final int length;

    public Path(@NotNull State state,
                @NotNull List<Point> points,
                @NotNull Point start,
                @NotNull Point end) {
        this.state = state;
        this.points = points;
        this.start = start;
        this.end = end;
        this.length = (int) ObjectIterables.size(points);
    }

    @NotNull
    public State state() {
        return state;
    }

    @NotNull
    public List<Point> points() {
        return points;
    }

    @NotNull
    public Point start() {
        return start;
    }

    @NotNull
    public Point end() {
        return end;
    }

    public int length() {
        return length;
    }

    public enum State {

        /**
         * Indicates that there is a valid path from start to end.
         */
        FOUND,

        /**
         * Indicates that there is not a valid path from start to end, but there is a path that might get close to the end.
         */
        BEST_EFFORT,

        /**
         * Indicates that no path could be found to get from start to end.
         */
        FAILED
    }
}