package net.minestom.server.entity;

import java.util.UUID;

public class EntityMob extends LivingEntity {

    public EntityMob(EntityType entityType) {
        super(entityType);
    }

    public EntityMob(EntityType entityType, UUID uuid) {
        super(entityType, uuid);
    }
}