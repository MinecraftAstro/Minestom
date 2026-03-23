package net.minestom.server.pathfinding.data;

import it.unimi.dsi.fastutil.objects.ObjectIterables;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Path {

    private final State state;

    private final List<PathPoint> positions;
    private final Point start;
    private final Point end;

    private final int length;

    public Path(@NotNull State state,
                @NotNull List<PathPoint> positions,
                @NotNull Point start,
                @NotNull Point end) {
        this.state = state;
        this.positions = positions;
        this.start = start;
        this.end = end;
        this.length = (int) ObjectIterables.size(positions);
    }

    @NotNull
    public State state() {
        return state;
    }

    @NotNull
    public List<PathPoint> positions() {
        return positions;
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