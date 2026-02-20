package net.minestom.server.pathfinding.validation.types;

import net.minestom.server.collision.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
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

    // TODO: fix closed doors
    // TODO: fix the issue where the first iteration results in updated nodes?
    // TODO: make stairs step and not jump type
    // TODO: cant pathfind in caves?

    @Override
    public @NotNull ValidationStatus checkValidity(@NotNull Node oldNode,
                                                   @NotNull Node newNode,
                                                   @NotNull MobContext mobContext) {
        final Point oldPoint = new Pos(oldNode.point().blockX() + 0.5, oldNode.point().y(), oldNode.point().blockZ() + 0.5);
        final Point newPoint = new Pos(newNode.point().blockX() + 0.5, newNode.point().y(), newNode.point().blockZ() + 0.5);
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

        if (mobContext.boundingBox().width() > 1.0D
                || mobContext.boundingBox().depth() > 1.0D) {
            return checkMoveWithLargeBoundingBox(context);
        } else {
            return checkMove(context);
        }
    }

    // check to make sure they aren't in a block that might have a door on one-side (will require additional checking)
    // check to make sure they have the clearance needed to move to the next position (nothing obstructs them from head to toe)
    // check to make sure that the block below the new point is solid (special cases like open doors or trapdoors will need to be handled differently)
    @NotNull
    private ValidationStatus checkMove(@NotNull ValidationContext context) {
        // TODO: handle cases where the entity is within a door or trapped door
        // TODO: handle cases where the entity might be against a fence or gate

        // check if the move is diagonal, if it is then we'll need to make sure that both of its neighbors are clear
        final Point direction = context.newPoint().sub(context.oldPoint());
        if (isDiagonalMove(direction)) {
            if (!hasClearance(context, context.oldPoint().add(direction.blockX(), 0, 0))
                    || !hasClearance(context, context.oldPoint().add(0, 0, direction.blockZ()))) {
                return new ValidationStatus(false);
            }
        }

        // check if they have the clearance needed to actually fit in this position without any obstructions
        if (!hasClearance(context, context.newPoint())) {
            // since they can't fit at the new spot, we'll check to see if they can step or jump to get in a position where they will fit
            System.out.println("No Clearance At: " + context.newPoint());
            return checkUpwardsMove(context);
        }

        // since we know they have clearance to go to this spot, check whether this move results in a fall
        return checkFallMove(context);
    }

    @NotNull
    private ValidationStatus checkMoveWithLargeBoundingBox(@NotNull ValidationContext context) {
        // TODO: support bounding boxes greater than a block in width/depth
        return new ValidationStatus(false);
    }

    @NotNull
    private ValidationStatus checkFallMove(@NotNull ValidationContext context) {
        // check if the blocks up to the max safe fall distance are air
        // this is our fast exit before needing expensive physics calls
        final int newPointX = context.newPoint().blockX();
        final int newPointY = context.newPoint().blockY();
        final int newPointZ = context.newPoint().blockZ();

        double landingY = Double.MIN_VALUE;
        boolean requiresPreciseCheck = false;
        for (int y = newPointY; y >= context.belowNewPoint().blockY() - MAXIMUM_FALL_DISTANCE; y--) {
            final Block block = context.instance().getBlock(newPointX, y, newPointZ, Block.Getter.Condition.TYPE);
            if (block != null && !block.isAir()) {
                final Shape blockShape = block.registry().collisionShape();
                if (blockShape.relativeStart().x() == 0.0D && blockShape.relativeStart().z() == 0.0D
                        && blockShape.relativeEnd().x() == 1.0D && blockShape.relativeEnd().z() == 1.0D) {
                    landingY = y + blockShape.relativeEnd().y();
                } else {
                    // setting the landingY is only to get over the first check outside of this loop
                    // technically we don't know if they can land at this Y value
                    landingY = y + blockShape.relativeEnd().y();
                    requiresPreciseCheck = true;
                }

                break;
            }
        }

        // check if there was any fall
        // if not, we don't have to update the node and they can just walk
        if (landingY == context.newPoint().y()) {
            return new ValidationStatus(true);
        }

        // check if the fall would exceed the max safe fall distance
        // we know it does if landingY doesn't get updated to an appropriate value
        if (landingY == Double.MIN_VALUE) {
            return new ValidationStatus(false);
        }

        // check if we need to do additional checks during the fall
        // this will happen because of weirdly sized blocks like trapdoors, etc...
        Node fallNode;
        if (requiresPreciseCheck) {
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
                    new Pos(newPointX, landingY, newPointZ),
                    context.newNode().start(),
                    context.newNode().target(),
                    context.newNode().depth() + 1
            );
        }

        return new ValidationStatus(true, fallNode);
    }

    @NotNull
    private ValidationStatus checkUpwardsMove(@NotNull ValidationContext context) {
        final Shape oldBlockShape = context.oldBlockShape();

        // gets the 2 blocks in front of the entity
        // since the jump height is 1.25, we need to be able to see if it's a block with a slab or a block with powdered snow, etc...
        final Shape newBlockShape = context.newBlockShape();
        final Shape aboveNewBlockShape = context.instance().getBlock(context.newPoint().add(0, 1, 0)).registry().collisionShape();

        final double totalBlockHeight = (newBlockShape.relativeEnd().y() + aboveNewBlockShape.relativeEnd().y()) - oldBlockShape.relativeEnd().y();
        System.out.println("Total Block Height: " + totalBlockHeight);

        if(totalBlockHeight > 0.0D && totalBlockHeight <= MAXIMUM_JUMP_HEIGHT) {
            System.out.println("Can step/jump");

            // check the clearance at the old point and the new point with the modified Y value from the step/jump
            if (!hasClearance(context, context.oldPoint().add(0, totalBlockHeight, 0))
                    || !hasClearance(context, context.newPoint().add(0, totalBlockHeight, 0))) {
                return new ValidationStatus(false);
            }

            final Node upwardsNode = new Node(
                    context.newPoint().add(0, totalBlockHeight, 0),
                    context.newNode().start(),
                    context.newNode().target(),
                    context.newNode().depth() + 1
            );
            upwardsNode.setType(totalBlockHeight <= MAXIMUM_STEP_HEIGHT ? Node.Type.STEP : Node.Type.JUMP);

            return new ValidationStatus(true, upwardsNode);
        }

        return new ValidationStatus(false);
    }

    private boolean hasClearance(@NotNull ValidationContext context,
                                 @NotNull Point point) {
        final BoundingBox.PointIterator blockIterator = context.boundingBox().getBlocks(point);
        while (blockIterator.hasNext()) {
            final BoundingBox.MutablePoint blockPoint = blockIterator.next();
            final Block block = context.instance().getBlock(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ(), Block.Getter.Condition.TYPE);

            if (block == null) continue;
            if (block.id() == Block.SCAFFOLDING.id()) continue;

            final boolean hit = block.registry().collisionShape().intersectBox(point.sub(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ()), context.boundingBox());
            if (hit) {
                System.out.println("Hit Block: " + block);
                return false;
            }
        }

        return true;
    }

    private boolean isDiagonalMove(@NotNull Point direction) {
        return Math.abs(direction.blockX()) == Math.abs(direction.blockZ());
    }
}