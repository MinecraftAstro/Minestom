package net.minestom.server.pathfinding.context;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public final class MobContext {

    // TODO: jump attributes, step attributes
    private final Instance instance;
    private final BoundingBox boundingBox;

    private final double maxSafeFallDistance;

    public MobContext(@NotNull Instance instance,
                      @NotNull BoundingBox boundingBox,
                      double maxSafeFallDistance) {
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

    public double maxSafeFallDistance() {
        return maxSafeFallDistance;
    }
}