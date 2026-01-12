package net.minestom.server.pathfinding.options;

import net.minestom.server.pathfinding.validation.NodeValidator;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// used to initialize a path generator call in the pathfinder
public final class PathfinderOptions {

    // TODO: option to load all chunks needed to generate path?

    private final boolean async;
    private final List<NodeValidator> nodeValidators;
    private final int maxIterations;

    private final int bloomFilterSize;
    private final double bloomFilterFpp;

    private PathfinderOptions(boolean async,
                              @NotNull List<NodeValidator> nodeValidators,
                              int maxIterations,
                              int bloomFilterSize,
                              double bloomFilterFpp) {
        this.async = async;
        this.nodeValidators = nodeValidators;
        Check.stateCondition(maxIterations <= 0, "Pathfinding max iterations must be greater than 0.");
        this.maxIterations = maxIterations;
        Check.stateCondition(bloomFilterSize <= 0, "Pathfinding bloom filter size must be greater than 0.");
        this.bloomFilterSize = bloomFilterSize;
        this.bloomFilterFpp = bloomFilterFpp;
    }

    public boolean async() {
        return async;
    }

    @NotNull
    public List<NodeValidator> nodeValidators() {
        return nodeValidators;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public int bloomFilterSize() {
        return bloomFilterSize;
    }

    public double bloomFilterFpp() {
        return bloomFilterFpp;
    }

    public static final class Builder {

        private boolean async;
        private List<NodeValidator> nodeValidators;
        private int maxIterations;

        private int bloomFilterSize;
        private double bloomFilterFpp;

        public Builder() {
            this.async = false;
            this.nodeValidators = new ArrayList<>();
            this.maxIterations = 100_000_000;
            this.bloomFilterSize = 1024;
            this.bloomFilterFpp = 0.01D;
        }

        @NotNull
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        @NotNull
        public Builder nodeValidator(@NotNull NodeValidator nodeValidator) {
            this.nodeValidators.add(nodeValidator);
            return this;
        }

        public Builder nodeValidators(@NotNull List<NodeValidator> nodeValidators) {
            this.nodeValidators = nodeValidators;
            return this;
        }

        @NotNull
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
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
        public PathfinderOptions build() {
            return new PathfinderOptions(
                    async,
                    List.copyOf(nodeValidators),
                    maxIterations,
                    bloomFilterSize,
                    bloomFilterFpp
            );
        }
    }
}