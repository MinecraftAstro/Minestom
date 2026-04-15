package net.minestom.server.pathfinding.evaluator.types;

import net.minestom.server.collision.*;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.coordinate.mutable.MutableVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.context.EvaluationContext;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.evaluator.result.NodeEvaluationResult;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import net.minestom.server.pathfinding.evaluator.NodeEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// a ground node evaluator that is designed with performance in mind
// mobs with bounding boxes that fit within a block will have improved performance using this evaluator with a bit of sacrifice for accuracy
// mobs with bounding boxes that don't fit within a block will still have improved performance, but they will require more computationally expensive checks
public final class FastGroundNodeEvaluator implements NodeEvaluator {

    // TODO: auto-load chunks
    // TODO: support water movement (just floating for now)
    // TODO: support larger hitboxes

    // TODO: make these dynamic based on a mob context
    private static final float MAXIMUM_STEP_HEIGHT = 0.6f;
    private static final float MAXIMUM_JUMP_HEIGHT = 1.25f;

    // TODO: fix the issue where the first iteration results in updated nodes?
    // TODO: make stairs step and not jump type

    // TODO: use mutable positions to avoid object churn

    @Override
    public @Nullable Node getValidStart(@NotNull MobContext mobContext,
                                        @NotNull Point target) {
        final Point startPoint = mobContext.startPoint();

        // check if the mob has clearance to spawn at the starting point
        // this will typically fail if the mob is stuck in a block
        if (!canFit(mobContext, startPoint)) {
            return null;
        }

        return new Node(startPoint, startPoint, target, 0);
    }

    @Override
    public @NotNull NodeEvaluationResult isValidMove(@NotNull Node oldNode,
                                                     @NotNull Node newNode,
                                                     @NotNull MobContext mobContext,
                                                     @NotNull PathfinderOptions options) {
        // center the points since our pathfinding works on going from the center of one block to another
        final Point oldPoint = new Pos(oldNode.point().centerBlockX(), oldNode.point().y(), oldNode.point().centerBlockZ());
        final Point newPoint = new Pos(newNode.point().centerBlockX(), newNode.point().y(), newNode.point().centerBlockZ());
        final Point belowNewPoint = newPoint.sub(0, 1, 0);

        // prevent NPEs caused by getting a block in an unloaded chunk
        final Block oldBlock;
        final Block newBlock;
        final Block belowNewBlock;
        try {
            oldBlock = mobContext.instance().getBlock(oldPoint, Block.Getter.Condition.TYPE);
            newBlock = mobContext.instance().getBlock(newPoint, Block.Getter.Condition.TYPE);
            belowNewBlock = mobContext.instance().getBlock(belowNewPoint, Block.Getter.Condition.TYPE);
        } catch (NullPointerException ignored) {
            return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
        }

        final Shape oldBlockShape = oldBlock.registry().collisionShape();
        final Shape newBlockShape = newBlock.registry().collisionShape();
        final Shape belowNewBlockShape = belowNewBlock.registry().collisionShape();

        final EvaluationContext evaluationContext = new EvaluationContext(
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

        if (isLargeBoundingBox(mobContext.boundingBox())) {
            return checkMoveForLargeBoundingBox(evaluationContext);
        } else {
            return checkMoveForSmallBoundingBox(evaluationContext);
        }
    }

    // check to make sure they aren't in a block that might have a door on one-side (will require additional checking)
    // check to make sure they have the clearance needed to move to the next position (nothing obstructs them from head to toe)
    // check to make sure that the block below the new point is solid (special cases like open doors or trapdoors will need to be handled differently)
    @NotNull
    private NodeEvaluationResult checkMoveForSmallBoundingBox(@NotNull EvaluationContext evaluationContext) {
        // TODO: handle cases where the entity is within a door or trapped door
        // TODO: handle cases where the entity might be against a fence or gate
        final MobContext mobContext = evaluationContext.mobContext();

        // check if the move is diagonal, if it is then we'll need to make sure that both of its neighbors are clear
        final Point direction = evaluationContext.newPoint().sub(evaluationContext.oldPoint());
        if (isDiagonalMove(direction)) {
            if (!canFit(mobContext, evaluationContext.oldPoint().add(direction.blockX(), 0, 0))
                    || !canFit(mobContext, evaluationContext.oldPoint().add(0, 0, direction.blockZ()))) {
                return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
            }
        }

        // check if they can actually stand at the new point without any obstructions
        // this won't catch all types of obstructions (such as closed doors, open trap-doors, etc) since the bounding box can technically fit in the center of block still
        if (!canFit(mobContext, evaluationContext.newPoint())) {
            // since they can't fit at the new point, we'll check to see if they can step or jump to get in a position where they will fit
            return checkUpwardsMove(evaluationContext);
        }

        // check if the new point has an open door, trap-door, or any other block that might not get caught from the above check
        final Shape newBlockShape = evaluationContext.newBlockShape();
        if (newBlockShape.relativeStart() != Vec.ZERO
                && newBlockShape.relativeEnd() != Vec.ZERO) {
            return checkUpwardsMove(evaluationContext);
        }

        // since we know they have clearance to go to this spot, check whether this move results in a fall
        return checkFallMoveForSmallBoundingBox(evaluationContext);
    }

    @NotNull
    private NodeEvaluationResult checkMoveForLargeBoundingBox(@NotNull EvaluationContext evaluationContext) {
        // since the bounding box is bigger than a block, we'll need to do swept AABB intersection instead of simple checks
        // this will be a bit more expensive, but not as expensive as full physics simulation
        if (hasBlockCollision(evaluationContext)) {
            // TODO: there is a block collision during this move, check if the entity can step or jump
            return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
        }

        // since we know they can move to this spot, check whether this move results in a fall
        return checkFallMoveForLargeBoundingBoxNew(evaluationContext);
    }

    @NotNull
    private NodeEvaluationResult checkFallMoveForSmallBoundingBox(@NotNull EvaluationContext evaluationContext) {
        // check if the blocks up to the max safe fall distance are air
        // this is our fast exit before needing expensive physics calls
        final int newPointX = evaluationContext.newPoint().blockX();
        final int newPointY = evaluationContext.newPoint().blockY();
        final int newPointZ = evaluationContext.newPoint().blockZ();

        double landingY = Double.MAX_VALUE;
        boolean requiresPreciseCheck = false;
        for (int y = newPointY; y >= evaluationContext.belowNewPoint().blockY() - evaluationContext.mobContext().safeFallDistance(); y--) {
            final Block block = evaluationContext.instance().getBlock(newPointX, y, newPointZ, Block.Getter.Condition.TYPE);
            if (block != null && !block.isAir()) {
                final Shape blockShape = block.registry().collisionShape();
                if (blockShape.relativeStart().x() == 0.0D && blockShape.relativeStart().z() == 0.0D
                        && blockShape.relativeEnd().x() == 1.0D && blockShape.relativeEnd().z() == 1.0D) {
                    // we know that this shape is consistent with a block that an entity could not fall through (it takes up the entire 1x1 space)
                    // we don't need to perform a precise physics check since we know this
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
        if (landingY == evaluationContext.newPoint().y()) {
            // store the block below the new block for future cost processing
            evaluationContext.newNode().setGroundBlock(evaluationContext.belowNewBlock());
            return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE);
        }

        // check if the fall would exceed the max safe fall distance
        // we know it does if landingY doesn't get updated to an appropriate value
        if (landingY == Double.MAX_VALUE) {
            return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
        }

        // check if we need to do additional checks during the fall
        // this will happen because of weirdly sized blocks like trapdoors, etc...
        Node fallNode;
        if (requiresPreciseCheck) {
            final Vec velocity = new Vec(0, -evaluationContext.mobContext().safeFallDistance(), 0);
            final PhysicsResult result = CollisionUtils.handlePhysics(
                    evaluationContext.instance(),
                    evaluationContext.boundingBox(),
                    evaluationContext.newPoint().asPos(),
                    velocity,
                    null,
                    true
            );

            if (!result.isOnGround())
                return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);

            fallNode = new Node(
                    new Pos(new Pos(newPointX, result.newPosition().y(), newPointZ)),
                    evaluationContext.newNode().start(),
                    evaluationContext.newNode().target(),
                    evaluationContext.newNode().depth() + 1
            );
        } else {
            fallNode = new Node(
                    new Pos(newPointX, landingY, newPointZ),
                    evaluationContext.newNode().start(),
                    evaluationContext.newNode().target(),
                    evaluationContext.newNode().depth() + 1
            );
        }

        fallNode.setGroundBlock(evaluationContext.instance().getBlock(fallNode.point(), Block.Getter.Condition.TYPE));
        return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE, fallNode);
    }

    @NotNull
    private NodeEvaluationResult checkFallMoveForLargeBoundingBoxNew(@NotNull EvaluationContext evaluationContext) {
        final Point oldPoint = evaluationContext.oldPoint();
        final Point newPoint = evaluationContext.newPoint();
        final Point direction = newPoint.sub(oldPoint).asVec().normalize();

        final BoundingBox boundingBox = evaluationContext.boundingBox();
        final int depth = (int) Math.max(Math.ceil(boundingBox.depth()), 1);
        final int width = (int) Math.max(Math.ceil(boundingBox.width()), 1);

//        System.out.println("Depth: " + depth);
//        System.out.println("Width: " + width);
//        System.out.println("Direction: " + direction);
//        System.out.println();


        final BoundingBox.PointIterator blockIterator = boundingBox.withOffset(new Vec(0, -1, 0)).getBlocks(newPoint);
        boolean allAir = true;
        while (blockIterator.hasNext()) {
            final MutableVec blockPoint = blockIterator.next();
            final Block block = evaluationContext.instance().getBlock(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ(), Block.Getter.Condition.TYPE);
            if (block != null && !block.isAir()) {
                allAir = false;
                break;
            }
        }

        if (allAir) {
            return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
        }

        return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE);
    }

    @NotNull
    private NodeEvaluationResult checkUpwardsMove(@NotNull EvaluationContext evaluationContext) {
        final Shape oldBlockShape = evaluationContext.oldBlockShape();

        // TODO: support step height for iron golems / camels

        // gets the 2 blocks in front of the entity
        // since the jump height is 1.25, we need to be able to see if it's a block with a slab or a block with powdered snow, etc...
        final Shape newBlockShape = evaluationContext.newBlockShape();
        final Shape aboveNewBlockShape = evaluationContext.instance().getBlock(evaluationContext.newPoint().add(0, 1, 0)).registry().collisionShape();

        final double totalBlockHeight = (newBlockShape.relativeEnd().y() + aboveNewBlockShape.relativeEnd().y()) - oldBlockShape.relativeEnd().y();
        //System.out.println("Total Block Height: " + totalBlockHeight);

        if (totalBlockHeight > 0.0D && totalBlockHeight <= MAXIMUM_JUMP_HEIGHT) {
            //System.out.println("Can step/jump");

            // check the clearance at the old point and the new point with the modified Y value from the step/jump
            if (!canFit(evaluationContext.mobContext(), evaluationContext.oldPoint().add(0, totalBlockHeight, 0))
                    || !canFit(evaluationContext.mobContext(), evaluationContext.newPoint().add(0, totalBlockHeight, 0))) {
                return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
            }

            final Node upwardsNode = new Node(
                    evaluationContext.newPoint().add(0, totalBlockHeight, 0),
                    evaluationContext.newNode().start(),
                    evaluationContext.newNode().target(),
                    evaluationContext.newNode().depth() + 1
            );
            upwardsNode.setType(totalBlockHeight <= MAXIMUM_STEP_HEIGHT ? Node.Type.STEP : Node.Type.JUMP);
            upwardsNode.setGroundBlock(evaluationContext.instance().getBlock(upwardsNode.point(), Block.Getter.Condition.TYPE));

            return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE, upwardsNode);
        }

        return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
    }

    private boolean canFit(@NotNull MobContext mobContext,
                           @NotNull Point point) {
        return CollisionUtils.canFit(
                mobContext.boundingBox(),
                mobContext.instance(),
                point
        );
    }

    private boolean hasBlockCollision(@NotNull EvaluationContext evaluationContext) {
        final Instance instance = evaluationContext.instance();
        final BoundingBox boundingBox = evaluationContext.boundingBox();
        final Point currentPoint = evaluationContext.oldPoint();
        final Point newPoint = evaluationContext.newPoint();

        final SweepResult result = new SweepResult(1, 0, 0, 0, null, 0, 0, 0, 0, 0, 0);

        // get the starting and ending coordinates of the bounding box and then get the broad-phase bounding box
        // if no collisions occur in the broad-phase bounding box then we don't need to perform an expensive AABB swept intersection check
        final double startMinX = currentPoint.x() + boundingBox.minX();
        final double startMinY = currentPoint.y() + boundingBox.minY();
        final double startMinZ = currentPoint.z() + boundingBox.minZ();
        final double startMaxX = currentPoint.x() + boundingBox.maxX();
        final double startMaxY = currentPoint.y() + boundingBox.maxY();
        final double startMaxZ = currentPoint.z() + boundingBox.maxZ();

        final double endMinX = newPoint.x() + boundingBox.minX();
        final double endMinY = newPoint.y() + boundingBox.minY();
        final double endMinZ = newPoint.z() + boundingBox.minZ();
        final double endMaxX = newPoint.x() + boundingBox.maxX();
        final double endMaxY = newPoint.y() + boundingBox.maxY();
        final double endMaxZ = newPoint.z() + boundingBox.maxZ();

        final double broadPhaseMinX = Math.min(startMinX, endMinX);
        final double broadPhaseMinY = Math.min(startMinY, endMinY);
        final double broadPhaseMinZ = Math.min(startMinZ, endMinZ);
        final double broadPhaseMaxX = Math.max(startMaxX, endMaxX);
        final double broadPhaseMaxY = Math.max(startMaxY, endMaxY);
        final double broadPhaseMaxZ = Math.max(startMaxZ, endMaxZ);

        final int minBlockX = (int) Math.floor(broadPhaseMinX);
        final int minBlockY = (int) Math.floor(broadPhaseMinY);
        final int minBlockZ = (int) Math.floor(broadPhaseMinZ);
        final int maxBlockX = (int) Math.ceil(broadPhaseMaxX);
        final int maxBlockY = (int) Math.ceil(broadPhaseMaxY);
        final int maxBlockZ = (int) Math.ceil(broadPhaseMaxZ);

        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = minBlockY; y <= maxBlockY; y++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    final BlockVec blockPoint = new BlockVec(x, y, z);
                    final Block block = instance.getBlock(blockPoint, Block.Getter.Condition.TYPE);

                    // this block will not have any possibility of a collision
                    if (block == null || block.id() == Block.AIR.id()) continue;

                    // check for the possibility of a collision
                    final Shape blockCollisionShape = block.registry().collisionShape();
                    if (blockCollisionShape.relativeStart() == Vec.ZERO
                            && blockCollisionShape.relativeEnd() == Vec.ZERO)
                        continue;

                    // check if the entity's bounding box collides with this block
                    final boolean hit = blockCollisionShape.intersectBoxSwept(
                            currentPoint,
                            newPoint.sub(currentPoint),
                            blockPoint,
                            boundingBox,
                            result
                    );

                    // if there is a collision, then just return true
                    if (hit) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isDiagonalMove(@NotNull Point direction) {
        return Math.abs(direction.blockX()) == Math.abs(direction.blockZ());
    }

    private boolean isLargeBoundingBox(@NotNull BoundingBox boundingBox) {
        return boundingBox.width() > 1.0D || boundingBox.depth() > 1.0D;
    }
}