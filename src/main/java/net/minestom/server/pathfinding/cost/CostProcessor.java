package net.minestom.server.pathfinding.cost;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public final class CostProcessor {

    // used to indicate that a move can not be taken at all
    // this is useful for avoiding blocks like lava, etc...
    public static final double ILLEGAL_MOVE_COST = Double.NEGATIVE_INFINITY;

    private final double walkCost;
    private final double fallCost;
    private final double stepCost;
    private final double jumpCost;

    private final Object2DoubleMap<Block> groundBlockCosts;

    private CostProcessor(double walkCost,
                          double fallCost,
                          double stepCost,
                          double jumpCost,
                          @NotNull Object2DoubleMap<Block> groundBlockCosts) {
        this.walkCost = walkCost;
        this.fallCost = fallCost;
        this.stepCost = stepCost;
        this.jumpCost = jumpCost;
        this.groundBlockCosts = groundBlockCosts;
    }

    // a negative g cost means that we should not go to this node no matter what
    public double calculateGCost(@NotNull Node parentNode,
                                 @NotNull Node node) {
        final double groundBlockCost = groundBlockCosts.getDouble(node.getGroundBlock());

        // make sure that the ground block would result in a legal move
        if (groundBlockCost == ILLEGAL_MOVE_COST) {
            return ILLEGAL_MOVE_COST;
        }

        final double g;
        switch (node.getType()) {
            case FALL -> g = parentNode.getG() + fallCost + groundBlockCost;
            case STEP -> g = parentNode.getG() + stepCost + groundBlockCost;
            case JUMP -> g = parentNode.getG() + jumpCost + groundBlockCost;
            default -> g = parentNode.getG() + walkCost + groundBlockCost;
        }

        return g;
    }

    public static final class Builder {

        private double walkCost;
        private double fallCost;
        private double stepCost;
        private double jumpCost;

        private final Object2DoubleMap<Block> groundBlockCosts;

        public Builder() {
            this.walkCost = 1.0D;
            this.fallCost = 1.0D;
            this.stepCost = 1.5D;
            this.jumpCost = 2.0D;
            this.groundBlockCosts = new Object2DoubleOpenHashMap<>();
        }

        @NotNull
        public Builder walkCost(double walkCost) {
            Check.stateCondition(walkCost < 0.0D, "Walk cost must be greater than or equal to 0.");
            this.walkCost = walkCost;
            return this;
        }

        @NotNull
        public Builder fallCost(double fallCost) {
            Check.stateCondition(fallCost < 0.0D, "Fall cost must be greater than or equal to 0.");
            this.fallCost = fallCost;
            return this;
        }

        @NotNull
        public Builder stepCost(double stepCost) {
            Check.stateCondition(stepCost < 0.0D, "Step cost must be greater than or equal to 0.");
            this.stepCost = stepCost;
            return this;
        }

        @NotNull
        public Builder jumpCost(double jumpCost) {
            Check.stateCondition(jumpCost < 0.0D, "Jump cost must be greater than or equal to 0.");
            this.jumpCost = jumpCost;
            return this;
        }

        @NotNull
        public Builder groundBlockCost(@NotNull Block block, double cost) {
            Check.stateCondition(block.compare(Block.AIR, Block.Comparator.ID), "Update fall cost instead of assigning a cost to an AIR ground block.");

            if (cost != CostProcessor.ILLEGAL_MOVE_COST)
                Check.stateCondition(cost < 0.0D, "Ground block cost must be greater than or equal to 0.");

            this.groundBlockCosts.put(block, cost);
            return this;
        }

        @NotNull
        public CostProcessor build() {
            return new CostProcessor(
                    walkCost,
                    fallCost,
                    stepCost,
                    jumpCost,
                    groundBlockCosts
            );
        }
    }
}