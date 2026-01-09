package net.minestom.server.pathfinding.data;

import it.unimi.dsi.fastutil.objects.ObjectIterables;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public final class Path implements Iterable<Point> {

    private final State state;

    private final Iterable<Point> positions;
    private final Point start;
    private final Point end;

    private final int length;

    public Path(@NotNull State state,
                @NotNull Iterable<Point> positions,
                @NotNull Point start,
                @NotNull Point end) {
        this.state = state;
        this.positions = positions;
        this.start = start;
        this.end = end;
        this.length = (int) ObjectIterables.size(positions);
    }

    @Override
    public @NotNull Iterator<Point> iterator() {
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
    public Collection<Point> collect() {
        final Collection<Point> collection = new ArrayList<>(length);
        positions.forEach(collection::add);
        return collection;
    }

    public enum State {

        FOUND,

        FAILED,

        BEST_EFFORT,

        MAX_ITERATIONS_REACHED
    }
}