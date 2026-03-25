package net.minestom.server.pathfinding.options;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.pathfinding.cost.CostProcessor;
import net.minestom.server.pathfinding.evaluator.types.FastGroundNodeEvaluator;
import net.minestom.server.pathfinding.movement.MovementStrategies;
import net.minestom.server.pathfinding.evaluator.NodeEvaluator;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

// used to initialize a path generator call in the pathfinder
public final class PathfinderOptions {

    private final boolean async;
    private final Collection<Vec> movementStrategy;
    private final NodeEvaluator nodeEvaluator;
    private final CostProcessor costProcessor;
    // 0 max iterations indicates that there is no max iterations for paths
    private final int maxIterations;
    // 0 max length indicates that there is no max length for paths
    private final int maxLength;

    private final int bloomFilterSize;
    private final double bloomFilterFpp;

    private final boolean bestEffortOnFailure;
    private final boolean bestEffortOnCancel;

    // TODO: this can be removed when Mode's chunk batch PR gets pushed (it never will)
    private final boolean autoLoadChunks;

    private final boolean debug;

    private PathfinderOptions(boolean async,
                              @NotNull Collection<Vec> movementStrategy,
                              @NotNull NodeEvaluator nodeEvaluator,
                              @NotNull CostProcessor costProcessor,
                              int maxIterations,
                              int maxLength,
                              int bloomFilterSize,
                              double bloomFilterFpp,
                              boolean bestEffortOnFailure,
                              boolean bestEffortOnCancel,
                              boolean autoLoadChunks,
                              boolean debug) {
        this.async = async;
        this.movementStrategy = movementStrategy;
        this.nodeEvaluator = nodeEvaluator;
        this.costProcessor = costProcessor;
        Check.stateCondition(maxIterations < 0, "Pathfinding max iterations must be greater than or equal to 0.");
        this.maxIterations = maxIterations;
        Check.stateCondition(maxLength < 0, "Pathfinding max length must be greater than or equal to 0.");
        this.maxLength = maxLength;
        Check.stateCondition(bloomFilterSize <= 0, "Pathfinding bloom filter size must be greater than 0.");
        this.bloomFilterSize = bloomFilterSize;
        this.bloomFilterFpp = bloomFilterFpp;
        this.bestEffortOnFailure = bestEffortOnFailure;
        this.bestEffortOnCancel = bestEffortOnCancel;
        this.autoLoadChunks = autoLoadChunks;
        this.debug = debug;
    }

    public boolean async() {
        return async;
    }

    @NotNull
    public Collection<Vec> movementStrategy() {
        return movementStrategy;
    }

    @NotNull
    public NodeEvaluator nodeEvaluator() {
        return nodeEvaluator;
    }

    @NotNull
    public CostProcessor costProcessor() {
        return costProcessor;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int bloomFilterSize() {
        return bloomFilterSize;
    }

    public double bloomFilterFpp() {
        return bloomFilterFpp;
    }

    public boolean isBestEffortOnFailure() {
        return bestEffortOnFailure;
    }

    public boolean isBestEffortOnCancel() {
        return bestEffortOnCancel;
    }

    public boolean isAutoLoadChunks() {
        return autoLoadChunks;
    }

    public boolean isDebug() {
        return debug;
    }

    public static final class Builder {

        private boolean async;
        private Collection<Vec> movementStrategy;
        private NodeEvaluator nodeEvaluator;
        private CostProcessor costProcessor;
        private int maxIterations;
        private int maxLength;

        private int bloomFilterSize;
        private double bloomFilterFpp;

        private boolean bestEffortOnFailure;
        private boolean bestEffortOnCancel;

        private boolean autoLoadChunks;

        private boolean debug;

        public Builder() {
            this.async = false;
            this.movementStrategy = MovementStrategies.BASIC_AND_DIAGONAL;
            this.nodeEvaluator = new FastGroundNodeEvaluator();
            this.costProcessor = new CostProcessor.Builder()
                    .build();
            this.maxIterations = 50_000;
            this.maxLength = 500;
            this.bloomFilterSize = 1024;
            this.bloomFilterFpp = 0.01D;
            this.bestEffortOnFailure = true;
            this.bestEffortOnCancel = false;
            this.autoLoadChunks = false;
            this.debug = false;
        }

        @NotNull
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        @NotNull
        public Builder movementStrategy(@NotNull Collection<Vec> movementStrategy) {
            this.movementStrategy = movementStrategy;
            return this;
        }

        @NotNull
        public Builder nodeEvaluator(@NotNull NodeEvaluator nodeEvaluator) {
            this.nodeEvaluator = nodeEvaluator;
            return this;
        }

        @NotNull
        public Builder costProcessor(@NotNull CostProcessor costProcessor) {
            this.costProcessor = costProcessor;
            return this;
        }

        @NotNull
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        @NotNull
        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        @NotNull
        public Builder bloomFilterSize(int bloomFilterSize) {
            this.bloomFilterSize = bloomFilterSize;
            return this;
        }

        @NotNull
        public Builder bloomFilterFpp(double bloomFilterFpp) {
            this.bloomFilterFpp = bloomFilterFpp;
            return this;
        }

        @NotNull
        public Builder bestEffortOnFailure(boolean bestEffortOnFailure) {
            this.bestEffortOnFailure = bestEffortOnFailure;
            return this;
        }

        @NotNull
        public Builder bestEffortOnCancel(boolean bestEffortOnCancel) {
            this.bestEffortOnCancel = bestEffortOnCancel;
            return this;
        }

        @NotNull
        public Builder autoLoadChunks(boolean autoLoadChunks) {
            this.autoLoadChunks = autoLoadChunks;
            return this;
        }

        @NotNull
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        @NotNull
        public PathfinderOptions build() {
            return new PathfinderOptions(
                    async,
                    List.copyOf(movementStrategy),
                    nodeEvaluator,
                    costProcessor,
                    maxIterations,
                    maxLength,
                    bloomFilterSize,
                    bloomFilterFpp,
                    bestEffortOnFailure,
                    bestEffortOnCancel,
                    autoLoadChunks,
                    debug
            );
        }
    }
}