package net.minestom.server.pathfinding.context;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

// important information we need for pathfinding and evaluating nodes
// we don't want to tightly couple our pathfinding code to entities, so we'll just ask for the required values
public final class MobContext {

    private final Instance instance;
    private final BoundingBox boundingBox;
    private final Point startPoint;

    private final double stepHeight;
    private final double jumpStrength;
    private final double safeFallDistance;

    public MobContext(@NotNull Instance instance,
                      @NotNull BoundingBox boundingBox,
                      @NotNull Point startPoint,
                      double stepHeight,
                      double jumpStrength,
                      double safeFallDistance) {
        this.instance = instance;
        this.boundingBox = boundingBox;
        this.startPoint = startPoint;
        this.stepHeight = stepHeight;
        this.jumpStrength = jumpStrength;
        this.safeFallDistance = safeFallDistance;
    }

    @NotNull
    public Instance instance() {
        return instance;
    }

    @NotNull
    public BoundingBox boundingBox() {
        return boundingBox;
    }

    @NotNull
    public Point startPoint() {
        return startPoint;
    }

    public double stepHeight() {
        return stepHeight;
    }

    public double jumpStrength() {
        return jumpStrength;
    }

    public double safeFallDistance() {
        return safeFallDistance;
    }
}