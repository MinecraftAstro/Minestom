package net.minestom.server.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.pathfinding.Pathfinder;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EntityMob extends LivingEntity {

    // TODO: path navigation
    // TODO: AI goals

    private final Pathfinder pathfinder;

    @Nullable
    private Path path;

    public EntityMob(EntityType entityType,
                     Pathfinder pathfinder) {
        super(entityType);
        this.pathfinder = pathfinder;
    }

    public EntityMob(EntityType entityType,
                     UUID uuid,
                     Pathfinder pathfinder) {
        super(entityType, uuid);
        this.pathfinder = pathfinder;
    }

    @Override
    public void update(long time) {
        // mob-related updates such as AI and path following
        // TODO

        // living entity updates
        super.update(time);
    }

    @ApiStatus.Internal
    public synchronized void tick() {

    }

    public CompletableFuture<Boolean> setPath(Point target) {
        // can't path outside the world border
        final WorldBorder worldBorder = instance.getWorldBorder();
        if(!worldBorder.inBounds(target)) {
            return CompletableFuture.completedFuture(false);
        }

        // TODO: do we need this with the block batches, or maybe load all chunks when pathfinding?
        // can't path in an unloaded chunk
        final Chunk chunk = instance.getChunkAt(target);
        if(!ChunkUtils.isLoaded(chunk)) {
            return CompletableFuture.completedFuture(false);
        }

        this.path = pathfinder.findPath(position, target, instance, boundingBox).join();

        return CompletableFuture.completedFuture(true);
    }

    public void clearPath() {
        this.path = null;
    }
}