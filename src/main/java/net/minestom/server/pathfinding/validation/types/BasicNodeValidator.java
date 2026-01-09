package net.minestom.server.pathfinding.validation.types;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.validation.NodeValidator;
import org.jetbrains.annotations.NotNull;

// TODO: only do swept checks for falls
public final class BasicNodeValidator implements NodeValidator {

    // TODO: make these dynamic based on a mob context
    private static final float MAXIMUM_STEP_HEIGHT = 0.6f;
    private static final float MAXIMUM_JUMP_HEIGHT = 1.25f;
    private static final float MAXIMUM_FALL_DISTANCE = 3.0f;

    // TODO: add support for diagonal movement
    @Override
    public boolean isValid(@NotNull Node oldNode,
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

        System.out.println("Has clearance: " + hasClearance(newPoint, instance, boundingBox));

        if ((oldBlockShape.relativeStart() == Vec.ZERO && oldBlockShape.relativeEnd() == Vec.ZERO)
                && (newBlockShape.relativeStart() == Vec.ZERO && newBlockShape.relativeEnd() == Vec.ZERO)
                && (belowNewBlock.isSolid())) {
            // we can quickly exit since we know this path will be clear
            return true;
        }

        System.out.println("Can Step: " + canStep(oldPoint, oldBlock, oldBlockShape, newPoint, newBlock, newBlockShape));
        System.out.println(newBlock);

        return false;
    }

    private boolean canStep(@NotNull Point oldPoint,
                            @NotNull Block oldBlock,
                            @NotNull Shape oldBlockShape,
                            @NotNull Point newPoint,
                            @NotNull Block newBlock,
                            @NotNull Shape newBlockShape) {
        final double oldY = oldPoint.y();
        final double newY = newPoint.y();
        final double yDifference = newY - oldY;

        // the blocks should be on the same Y-level or else we know we can't step
        if (yDifference != 0.0D) {
            return false;
        }

        final double oldShapeEndY = oldBlockShape.relativeEnd().y();
        final double newShapeEndY = newBlockShape.relativeEnd().y();
        final double yEndDifference = newShapeEndY - oldShapeEndY;
        System.out.println(yEndDifference);
        if (yEndDifference <= 0.0D || yEndDifference > MAXIMUM_STEP_HEIGHT) {
            return false;
        }

        // TODO: check for clearance on old point and new point (aka is the step possible)

        return true;
    }

    private boolean canJump() {
        return false;
    }

    private boolean canFall() {
        return false;
    }

    private boolean hasClearance(@NotNull Point point,
                                 @NotNull Instance instance,
                                 @NotNull BoundingBox boundingBox) {
        final int height = (int) Math.ceil(boundingBox.height());
        System.out.println("Height: " + height);

        final int initialY = point.blockY();
        for (int y = initialY; y <= initialY + height; y++) {
            final Point blockPoint = point.withY(y);
            final Block block = instance.getBlock(blockPoint, Block.Getter.Condition.TYPE);
            System.out.println(block);
            final Shape blockShape = block.registry().collisionShape();
            if (blockShape.intersectBox(point.sub(blockPoint), boundingBox)) {
                System.out.println("Does not have clearance!");
                return false;
            }
        }

        return true;
    }
}