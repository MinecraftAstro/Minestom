package net.minestom.server.entity.ai;

import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.ai.navigation.PathNavigator;
import net.minestom.server.entity.ai.navigation.types.OldGroundPathNavigator;
import net.minestom.server.pathfinding.Pathfinder;
import org.jetbrains.annotations.ApiStatus;

/**
 * Holds short and long-lived AI state for an {@link EntityMob}.
 */
public final class Brain {

    private final EntityMob entityMob;

    private final PathNavigator pathNavigator;

    public Brain(EntityMob entityMob,
                 PathNavigator.Type pathNavigatorType) {
        this.entityMob = entityMob;
        switch (pathNavigatorType) {
            default -> this.pathNavigator = new OldGroundPathNavigator(entityMob);
        }
    }

    public Brain(EntityMob entityMob,
                 PathNavigator.Type pathNavigatorType,
                 Pathfinder pathfinder) {
        this.entityMob = entityMob;
        switch (pathNavigatorType) {
            default -> this.pathNavigator = new OldGroundPathNavigator(entityMob, pathfinder);
        }
    }

    @ApiStatus.Internal
    public synchronized void tick() {
        // handle pathfinding
        pathNavigator.tick();
    }

    public PathNavigator pathNavigator() {
        return pathNavigator;
    }
}