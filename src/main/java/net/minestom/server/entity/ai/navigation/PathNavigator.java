package net.minestom.server.entity.ai.navigation;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.pathfinding.Pathfinder;
import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.pathfinding.data.PathPoint;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class PathNavigator {

    protected final EntityMob entityMob;

    private final Pathfinder pathfinder;

    @Nullable
    protected Path currentPath;
    protected List<PathPoint> pathPoints = List.of();
    protected int currentIndex = 0;

    // set-up the default options for pathfinding completion
    private int completionRange = 1;
    private Runnable completionCallback = () -> {
    };

    public PathNavigator(@NotNull EntityMob entityMob,
                         @NotNull Pathfinder pathfinder) {
        this.entityMob = entityMob;
        this.pathfinder = pathfinder;
        this.currentPath = null;
    }

    public PathNavigator(@NotNull EntityMob entityMob) {
        this(entityMob, Pathfinder.DEFAULT_PATHFINDER);
    }

    protected abstract void navigatePath();

    @ApiStatus.Internal
    public synchronized void tick() {
        if (entityMob.isDead())
            return;

        if (currentPath == null)
            return;

        // check if the mob is within the completion distance of the path's end point
        if (entityMob.getPosition().manhattanDistance(currentPath.end()) <= completionRange) {
            completionCallback.run();
            clearPath();
            return;
        }

        navigatePath();
    }

    public CompletableFuture<Path> setPath(Point target,
                                           int completionRange,
                                           Runnable completionCallback) {
        final Instance instance = entityMob.getInstance();
        final Point position = entityMob.getPosition();

        // can't path outside the world border
        final WorldBorder worldBorder = instance.getWorldBorder();
        if (!worldBorder.inBounds(target)) {
            return CompletableFuture.completedFuture(new Path(Path.State.FAILED, List.of(), position, target));
        }

        // TODO: do we need this with the block batches, or maybe load all chunks when pathfinding?
        // can't path in an unloaded chunk
        final Chunk chunk = instance.getChunkAt(target);
        if (!ChunkUtils.isLoaded(chunk)) {
            return CompletableFuture.completedFuture(new Path(Path.State.FAILED, List.of(), position, target));
        }

        final CompletableFuture<Path> pathFuture = pathfinder.findPath(
                position,
                target,
                new MobContext(instance, entityMob.getBoundingBox(), entityMob.getAttributeValue(Attribute.SAFE_FALL_DISTANCE)),
                completionRange
        );

        pathFuture.whenComplete((path, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }

            synchronized (this) {
                this.currentIndex = 0;
                this.pathPoints = path.list();
                this.completionRange = completionRange;
                this.completionCallback = completionCallback;
                this.currentPath = path;
            }
        });

        return pathFuture;
    }

    /**
     * Clears the mob's current path which forces it to stop navigating the path.
     */
    public synchronized void clearPath() {
        this.currentPath = null;
        this.currentIndex = 0;
        this.pathPoints = List.of();
    }

    public enum Type {

        GROUND
    }
}