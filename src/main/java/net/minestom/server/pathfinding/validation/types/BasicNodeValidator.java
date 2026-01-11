package net.minestom.server.pathfinding.validation.types;

import net.minestom.server.collision.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.pathfinding.context.ValidationContext;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.validation.NodeValidator;
import net.minestom.server.pathfinding.validation.ValidationStatus;
import org.jetbrains.annotations.NotNull;

// TODO: only do swept checks for falls
public final class BasicNodeValidator implements NodeValidator {

    // TODO: make these dynamic based on a mob context
    private static final float MAXIMUM_STEP_HEIGHT = 0.6f;
    private static final float MAXIMUM_JUMP_HEIGHT = 1.25f;
    private static final int MAXIMUM_FALL_DISTANCE = 3;

    // TODO: add support for diagonal movement
    @Override
    public @NotNull ValidationStatus checkValidity(@NotNull Node oldNode,
                                                   @NotNull Node newNode,
                                                   @NotNull Instance instance,
                                                   @NotNull BoundingBox boundingBox) {
        final Point oldPoint = oldNode.point();
        final Point newPoint = newNode.point();
        final Point belowNewPoint = newNode.point().sub(0, 1, 0);

        final Block oldBlock = instance.getBlock(oldPoint, Block.Getter.Condition.TYPE);
        final Block newBlock = instance.getBlock(newPoint, Block.Getter.Condition.TYPE);
        final Block belowNewBlock = instance.getBlock(belowNewPoint, Block.Getter.Condition.TYPE);

        final Shape oldBlockShape = oldBlock.registry().collisionShape();
        final Shape newBlockShape = newBlock.registry().collisionShape();
        final Shape belowNewBlockShape = belowNewBlock.registry().collisionShape();

        final ValidationContext context = new ValidationContext(
                instance,
                boundingBox,
                oldNode,
                newNode,
                oldPoint,
                newPoint,
                belowNewPoint,
                oldBlock,
                newBlock,
                belowNewBlock,
                oldBlockShape,
                newBlockShape,
                belowNewBlockShape
        );

        return checkMove(context);
    }

    // check to make sure they aren't in a block that might have a door on one-side (will require additional checking)
    // check to make sure they have the clearance needed to move to the next position (nothing obstructs them from head to toe)
    // check to make sure that the block below the new point is solid (special cases like open doors or trapdoors will need to be handled differently)
    @NotNull
    private ValidationStatus checkMove(@NotNull ValidationContext context) {
        // this is not a common occurrence, so we can use an expensive physics call to test
        // this only happens if the mob were to spawn on a block that had a door or something similar
        final boolean withinBlock = !(context.oldBlockShape().relativeStart() == Vec.ZERO && context.oldBlockShape().relativeEnd() == Vec.ZERO);
        if (withinBlock) {
            System.out.println("Expensive call 1");
            final Vec velocity = context.newPoint().sub(context.oldPoint()).asVec();
            final PhysicsResult result = CollisionUtils.handlePhysics(
                    context.instance(),
                    context.boundingBox(),
                    context.oldPoint().asPos(),
                    velocity,
                    null,
                    true
            );

            if (result.hasCollision())
                return new ValidationStatus(false);
        }

        // check if the block below the new block is fully solid
        // if there is even a minor height difference or potential to fall, this will fail, and we will need to check for any potential falls
        final boolean solidBelowNewBlock = context.belowNewBlockShape().isFaceFull(BlockFace.TOP);
        if (!solidBelowNewBlock) {
            return checkFall(context);
        }

        // check if they have clearance to actually fit in this position without any obstructions
        // if they can't fit at the new spot, then we'll have to check if they can step or jump to get in a position where they will fit
        final boolean hasClearance = hasClearance(context);
        if (!hasClearance) {
            return checkUpwards(context);
        }

        return new ValidationStatus(true);
    }

    @NotNull
    private ValidationStatus checkFall(@NotNull ValidationContext context) {
        // check if the blocks up to the max safe fall distance are air
        // this is our fast exit before needing expensive physics calls
        final int newPointX = context.belowNewPoint().blockX();
        final int newPointY = context.belowNewPoint().blockY();
        final int newPointZ = context.belowNewPoint().blockZ();

        int landingY = Integer.MIN_VALUE;
        for (int y = newPointY; y > context.belowNewPoint().blockY() - MAXIMUM_FALL_DISTANCE; y--) {
            final Block block = context.instance().getBlock(newPointX, y, newPointZ, Block.Getter.Condition.TYPE);
            // TODO: check for special blocks like trapdoors
            if (!block.isAir()) {
                landingY = y;
                break;
            }
        }

        if (landingY == Integer.MIN_VALUE) {
            return new ValidationStatus(false);
        }

        // check if the block at the bottom of the max safe fall distance is a solid block
        final boolean solidEndingBlock = context.instance().getBlock(newPointX, landingY, newPointZ, Block.Getter.Condition.TYPE).registry().collisionShape().isFaceFull(BlockFace.TOP);
        if (!solidEndingBlock) {
            // TODO: do more checks
            return new ValidationStatus(false);
        }

        final Node fallNode = new Node(
                new Pos(newPointX, landingY + 1, newPointZ),
                context.newNode().start(),
                context.newNode().target(),
                context.newNode().depth() + 1
        );
        return new ValidationStatus(true, fallNode);
    }

    @NotNull
    private ValidationStatus checkUpwards(@NotNull ValidationContext context) {
        final double oldY = context.oldPoint().y();
        final double newY = context.newPoint().y();
        final double yDifference = newY - oldY;

        // the blocks should be on the same Y-level or else we know we can't go up
        if (yDifference != 0.0D) {
            return new ValidationStatus(false);
        }

        final double oldShapeEndY = context.oldBlockShape().relativeEnd().y();
        final double newShapeEndY = context.newBlockShape().relativeEnd().y();
        final double yEndDifference = newShapeEndY - oldShapeEndY;
        System.out.println(yEndDifference);

        if (yEndDifference <= 0.0D) {
            // this should never happen, but if it does then we don't care about it since this would be a fall
            return new ValidationStatus(false);
        }

        if (yEndDifference <= MAXIMUM_STEP_HEIGHT) {
            // we can achieve this movement with a step
            // TODO: check for clearance on the old point and new point (is the step or jump actually possible with the bounding box)
        }

        if (yEndDifference <= MAXIMUM_JUMP_HEIGHT) {
            // we can achieve this movement with a jump
            // TODO: check for clearance on the old point and new point (is the step or jump actually possible with the bounding box)
        }

        return new ValidationStatus(false);
    }

    private boolean hasClearance(@NotNull ValidationContext context) {
        final int height = (int) Math.ceil(context.boundingBox().height());
        //System.out.println("Height: " + height);

        final int initialY = context.newPoint().blockY();
        for (int y = initialY; y <= initialY + height; y++) {
            final Point blockPoint = context.newPoint().withY(y);
            final Block block = context.instance().getBlock(blockPoint, Block.Getter.Condition.TYPE);
            //System.out.println(block);
            final Shape blockShape = block.registry().collisionShape();
            if (blockShape.intersectBox(context.newPoint().sub(blockPoint), context.boundingBox())) {
                System.out.println("Does not have clearance!");
                return false;
            }
        }

        return true;
    }
}