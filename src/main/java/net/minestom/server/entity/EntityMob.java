package net.minestom.server.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.ai.Brain;
import net.minestom.server.entity.ai.navigation.PathNavigator;
import net.minestom.server.pathfinding.Pathfinder;
import net.minestom.server.pathfinding.data.Path;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EntityMob extends LivingEntity {

    private final Brain brain;

    public EntityMob(EntityType entityType,
                     PathNavigator.Type pathNavigatorType) {
        super(entityType);
        this.brain = new Brain(this, pathNavigatorType);
    }

    public EntityMob(EntityType entityType,
                     PathNavigator.Type pathNavigatorType,
                     Pathfinder pathfinder) {
        super(entityType);
        this.brain = new Brain(this, pathNavigatorType, pathfinder);
    }

    public EntityMob(EntityType entityType,
                     UUID uuid,
                     PathNavigator.Type pathNavigatorType) {
        super(entityType, uuid);
        this.brain = new Brain(this, pathNavigatorType);
    }

    public EntityMob(EntityType entityType,
                     UUID uuid,
                     PathNavigator.Type pathNavigatorType,
                     Pathfinder pathfinder) {
        super(entityType, uuid);
        this.brain = new Brain(this, pathNavigatorType, pathfinder);
    }

    @Override
    public void update(long time) {
        // mob-related updates such as AI and path navigation
        brain.tick();

        // living entity updates
        super.update(time);
    }

    public CompletableFuture<Path> setPath(Point target,
                                           int completionRange,
                                           Runnable completionCallback) {
        return brain.pathNavigator().setPath(target, completionRange, completionCallback);
    }

    public void clearPath() {
        brain.pathNavigator().clearPath();
    }
}