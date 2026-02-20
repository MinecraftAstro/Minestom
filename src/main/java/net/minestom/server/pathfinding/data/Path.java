package net.minestom.server.pathfinding.data;

import it.unimi.dsi.fastutil.objects.ObjectIterables;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Path implements Iterable<PathPoint> {

    private final State state;

    private final Iterable<PathPoint> positions;
    private final Point start;
    private final Point end;

    private final int length;

    public Path(@NotNull State state,
                @NotNull Iterable<PathPoint> positions,
                @NotNull Point start,
                @NotNull Point end) {
        this.state = state;
        this.positions = positions;
        this.start = start;
        this.end = end;
        this.length = (int) ObjectIterables.size(positions);
    }

    @Override
    public @NotNull Iterator<PathPoint> iterator() {
        return positions.iterator();
    }

    @NotNull
    public State state() {
        return state;
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

    @NotNull
    public List<PathPoint> list() {
        final List<PathPoint> list = new ArrayList<>(length);
        positions.forEach(list::add);
        return list;
    }

    public enum State {

        FOUND,

        FAILED,

        BEST_EFFORT,

        MAX_ITERATIONS_REACHED
    }
}