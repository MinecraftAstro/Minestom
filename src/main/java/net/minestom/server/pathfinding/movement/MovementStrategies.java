package net.minestom.server.pathfinding.movement;

import net.minestom.server.coordinate.Vec;

import java.util.Arrays;

public final class MovementStrategies {

    private MovementStrategies() {
    }

    public static final Iterable<Vec> BASIC_MOVEMENT_STRATEGY = Arrays.asList(
            new Vec(-1, 0, 0),
            new Vec(0, 0, -1),
            new Vec(0, 0, 1),
            new Vec(1, 0, 0)
    );

    public static final Iterable<Vec> DIAGONAL_MOVEMENT_STRATEGY = Arrays.asList(
            new Vec(-1, 0, -1),
            new Vec(-1, 0, 0),
            new Vec(-1, 0, 1),
            new Vec(0, 0, -1),
            new Vec(0, 0, 1),
            new Vec(1, 0, -1),
            new Vec(1, 0, 0),
            new Vec(1, 0, 1)
    );
}