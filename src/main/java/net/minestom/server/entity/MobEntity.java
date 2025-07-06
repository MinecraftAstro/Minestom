package net.minestom.server.entity;

import net.minestom.server.entity.pathfinding.path.PathWalker;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MobEntity extends LivingEntity implements PathWalker {

    public MobEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    public MobEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    @Override
    public void update(long time) {
        // TODO: pathfinding
        // TODO: AI goals

        // entity updates
        super.update(time);
    }
}