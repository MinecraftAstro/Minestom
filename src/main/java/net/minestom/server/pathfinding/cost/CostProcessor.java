package net.minestom.server.pathfinding.cost;

import org.jetbrains.annotations.NotNull;

public final class CostProcessor {

    // TODO: can we add costs for certain ground blocks, etc?

    private final double walkCost;
    private final double fallCost;
    private final double stepCost;
    private final double jumpCost;

    private CostProcessor(double walkCost,
                          double fallCost,
                          double stepCost,
                          double jumpCost) {
        this.walkCost = walkCost;
        this.fallCost = fallCost;
        this.stepCost = stepCost;
        this.jumpCost = jumpCost;
    }

    public double getWalkCost() {
        return walkCost;
    }

    public double getFallCost() {
        return fallCost;
    }

    public double getStepCost() {
        return stepCost;
    }

    public double getJumpCost() {
        return jumpCost;
    }

    public static final class Builder {

        private double walkCost;
        private double fallCost;
        private double stepCost;
        private double jumpCost;

        public Builder() {
            this.walkCost = 0.0D;
            this.fallCost = 0.0D;
            this.stepCost = 0.5D;
            this.jumpCost = 2.0D;
        }

        @NotNull
        public Builder walkCost(double walkCost) {
            this.walkCost = walkCost;
            return this;
        }

        @NotNull
        public Builder fallCost(double fallCost) {
            this.fallCost = fallCost;
            return this;
        }

        @NotNull
        public Builder stepCost(double stepCost) {
            this.stepCost = stepCost;
            return this;
        }

        @NotNull
        public Builder jumpCost(double jumpCost) {
            this.jumpCost = jumpCost;
            return this;
        }

        @NotNull
        public CostProcessor build() {
            return new CostProcessor(
                    walkCost,
                    fallCost,
                    stepCost,
                    jumpCost
            );
        }
    }
}