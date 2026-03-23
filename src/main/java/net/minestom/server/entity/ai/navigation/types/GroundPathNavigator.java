package net.minestom.server.entity.ai.navigation.types;

import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.ai.navigation.PathNavigator;
import net.minestom.server.pathfinding.Pathfinder;
import org.jetbrains.annotations.NotNull;

public final class GroundPathNavigator extends PathNavigator {

    public GroundPathNavigator(@NotNull EntityMob entityMob, @NotNull Pathfinder pathfinder) {
        super(entityMob, pathfinder);
    }

    public GroundPathNavigator(@NotNull EntityMob entityMob) {
        super(entityMob);
    }

    @Override
    protected void navigatePath() {

    }
}