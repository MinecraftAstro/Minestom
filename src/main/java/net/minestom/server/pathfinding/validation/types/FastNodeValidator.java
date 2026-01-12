package net.minestom.server.pathfinding.validation.types;

import net.minestom.server.collision.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.context.ValidationContext;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.validation.NodeValidator;
import net.minestom.server.pathfinding.validation.ValidationStatus;
import org.jetbrains.annotations.NotNull;

public final class FastNodeValidator implements NodeValidator {

    // TODO: make these dynamic based on a mob context
    private static final float MAXIMUM_STEP_HEIGHT = 0.6f;
    private static final float MAXIMUM_JUMP_HEIGHT = 1.25f;
    private static final int MAXIMUM_FALL_DISTANCE = 3;

    // TODO: add support for diagonal movement
    @Override
    public @NotNull ValidationStatus checkValidity(@NotNull Node oldNode,
                                                   @NotNull Node newNode,
                                                   @NotNull MobContext mobContext) {
        final Point oldPoint = oldNode.point();
        final Point newPoint = newNode.point();
        final Point belowNewPoint = newNode.point().sub(0, 1, 0);

        final Block oldBlock = mobContext.instance().getBlock(oldPoint, Block.Getter.Condition.TYPE);
        final Block newBlock = mobContext.instance().getBlock(newPoint, Block.Getter.Condition.TYPE);
        final Block belowNewBlock = mobContext.instance().getBlock(belowNewPoint, Block.Getter.Condition.TYPE);

        final Shape oldBlockShape = oldBlock.registry().collisionShape();
        final Shape newBlockShape = newBlock.registry().collisionShape();
        final Shape belowNewBlockShape = belowNewBlock.registry().collisionShape();

        final ValidationContext context = new ValidationContext(
                mobContext,
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
        boolean requiresAdditionalChecks = false;
        for (int y = newPointY; y > context.belowNewPoint().blockY() - MAXIMUM_FALL_DISTANCE; y--) {
            final Block block = context.instance().getBlock(newPointX, y, newPointZ, Block.Getter.Condition.TYPE);
            if (block != null && !block.isAir()) {
                final Shape blockShape = block.registry().collisionShape();
                if (blockShape.relativeStart().x() == 0.0D && blockShape.relativeStart().z() == 0.0D
                        && blockShape.relativeEnd().x() == 1.0D && blockShape.relativeEnd().z() == 1.0D) {
                    landingY = y;
                } else {
                    // setting the landingY is only to get over the first check outside of this loop
                    // technically we don't know if they can land at this Y value
                    landingY = y;
                    requiresAdditionalChecks = true;
                }

                break;
            }
        }

        // check if the fall would exceed the max safe fall distance
        // we know it does if landingY doesn't get updated to an appropriate value
        if (landingY == Integer.MIN_VALUE) {
            return new ValidationStatus(false);
        }

        // check if we need to do additional checks during the fall
        // this will happen because of weirdly sized blocks like trapdoors, etc...
        Node fallNode;
        if (requiresAdditionalChecks) {
            System.out.println("Expensive call 2");
            final Vec velocity = new Vec(0, -MAXIMUM_FALL_DISTANCE, 0);
            final PhysicsResult result = CollisionUtils.handlePhysics(
                    context.instance(),
                    context.boundingBox(),
                    context.newPoint().asPos(),
                    velocity,
                    null,
                    true
            );

            if (!result.isOnGround())
                return new ValidationStatus(false);

            fallNode = new Node(
                    new Pos(new Pos(newPointX, result.newPosition().y(), newPointZ)),
                    context.newNode().start(),
                    context.newNode().target(),
                    context.newNode().depth() + 1
            );
        } else {
            fallNode = new Node(
                    new Pos(newPointX, landingY + 1, newPointZ),
                    context.newNode().start(),
                    context.newNode().target(),
                    context.newNode().depth() + 1
            );
        }

        return new ValidationStatus(true, fallNode);
    }

    @NotNull
    private ValidationStatus checkUpwards(@NotNull ValidationContext context) {
        final Shape newBlockShape = context.newBlockShape();
        final Shape aboveNewBlockShape = context.instance().getBlock(context.newPoint().add(0, 1, 0)).registry().collisionShape();

        if(aboveNewBlockShape.relativeEnd().y() == 0.0D) {
            // TODO: check for clearance standing at pos
            final double newBlockHeight = newBlockShape.relativeEnd().y();
            if(newBlockHeight <= MAXIMUM_STEP_HEIGHT) {
                // TODO: step
                if(hasClearance(context, newBlockHeight)) {
                    System.out.println("Can step!");
                }
            } else {
                // TODO: jump
            }
        } else {
            // TODO: make sure it's less than or equal to 0.25 and then check for clearance at pos
            // TODO: handle cases like doors and trapdoors that are open
        }

        return new ValidationStatus(false);
    }

    private boolean hasClearance(@NotNull ValidationContext context) {
        return hasClearance(context, 0.0D);
    }

    private boolean hasClearance(@NotNull ValidationContext context,
                                 double heightOffset) {
        final int height = (int) Math.ceil(context.boundingBox().height() + heightOffset);

        final int initialY = context.newPoint().blockY();
        for (int y = initialY; y <= initialY + height; y++) {
            final Point blockPoint = context.newPoint().withY(y);
            final Block block = context.instance().getBlock(blockPoint, Block.Getter.Condition.TYPE);
            final Shape blockShape = block.registry().collisionShape();
            if (blockShape.intersectBox(context.newPoint().sub(blockPoint), context.boundingBox())) {
                return false;
            }
        }

        return true;
    }
}