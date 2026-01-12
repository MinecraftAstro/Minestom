package net.minestom.server.pathfinding.context;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public final class MobContext {

    // TODO: jump attributes, step attributes
    private final Instance instance;
    private final BoundingBox boundingBox;

    private final int maxSafeFallDistance;

    public MobContext(@NotNull Instance instance,
                      @NotNull BoundingBox boundingBox,
                      int maxSafeFallDistance) {
        this.instance = instance;
        this.boundingBox = boundingBox;
        this.maxSafeFallDistance = maxSafeFallDistance;
    }

    public Instance instance() {
        return instance;
    }

    public BoundingBox boundingBox() {
        return boundingBox;
    }

    public int maxSafeFallDistance() {
        return maxSafeFallDistance;
    }
}