package net.minestom.server.pathfinding.options;

import net.minestom.server.pathfinding.generator.NodeGenerator;
import net.minestom.server.pathfinding.generator.types.WalkNodeGenerator;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

// used to initialize a path generator call in the pathfinder
public final class PathfinderOptions {

    // world type (static or dynamic world)
    // average path distance (to prevent priority queue resizing)
    // completion range (how close we need to get to the target in order to finish the pathfinding)
    // completion callback (what happens when the path is completed)
    // node generator (the generator we should use when generating nodes for paths)

    private final WorldType worldType;
    private final NodeGenerator nodeGenerator;

    private final double completionRange;
    private final Runnable completionCallback;

    private PathfinderOptions(@NotNull WorldType worldType,
                              @NotNull NodeGenerator nodeGenerator,
                              double completionRange,
                              @NotNull Runnable completionCallback) {
        this.worldType = worldType;
        this.nodeGenerator = nodeGenerator;
        Check.stateCondition(completionRange > 0, "Pathfinding completion range must be greater than 0.");
        this.completionRange = completionRange;
        this.completionCallback = completionCallback;
    }

    @NotNull
    public WorldType worldType() {
        return worldType;
    }

    @NotNull
    public NodeGenerator nodeGenerator() {
        return nodeGenerator;
    }

    public double completionRange() {
        return completionRange;
    }

    @NotNull
    public Runnable completionCallback() {
        return completionCallback;
    }

    public enum WorldType {

        /**
         * A static world means that the world will not change (block removals/additions) while pathfinding is performed.
         * This isn't a strict requirement that is imposed, but just beware that pathfinding could break (might not be able to complete a path).
         * Without having to worry about the world changing we are able to simplify and optimize some portions of the pathfinding.
         */
        STATIC,

        /**
         * A dynamic world means that the world will change (block removals/additions) while pathfinding is performed.
         * Some extra logic will need to be implemented during entity path following to make sure that paths can be completed.
         * This is the default for pathfinding.
         */
        DYNAMIC
    }

    public static final class Builder {

        private WorldType worldType;
        private NodeGenerator nodeGenerator;

        private double completionRange;
        private Runnable completionCallback;

        public Builder() {
            this.worldType = WorldType.DYNAMIC;
            this.nodeGenerator = new WalkNodeGenerator();
            this.completionRange = 1.0D;
            this.completionCallback = () -> {
            };
        }

        @NotNull
        public Builder worldType(@NotNull WorldType worldType) {
            this.worldType = worldType;
            return this;
        }

        @NotNull
        public Builder nodeGenerator(@NotNull NodeGenerator nodeGenerator) {
            this.nodeGenerator = nodeGenerator;
            return this;
        }

        @NotNull
        public Builder completionRange(double completionRange) {
            this.completionRange = completionRange;
            return this;
        }

        @NotNull
        public Builder completionCallback(@NotNull Runnable completionCallback) {
            this.completionCallback = completionCallback;
            return this;
        }

        @NotNull
        public PathfinderOptions build() {
            return new PathfinderOptions(worldType, nodeGenerator, completionRange, completionCallback);
        }
    }
}