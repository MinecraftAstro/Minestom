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

    // TODO: fix issue when the mob starts in something like a stair-case where it doesnt move
    // it seems to want to have a node within the stair-case, which causes the entity to not move

    // TODO: auto-load chunks
    // TODO: support water movement (just floating for now)
    // TODO: support larger hitboxes

    // TODO: make these dynamic based on a mob context
    private static final float MAXIMUM_STEP_HEIGHT = 0.6f;
    private static final float MAXIMUM_JUMP_HEIGHT = 1.25f;

    // TODO: fix closed doors
    // TODO: fix the issue where the first iteration results in updated nodes?
    // TODO: make stairs step and not jump type
    // TODO: cant pathfind in caves?

    // TODO: use mutable positions to avoid object churn

    // TODO: prevent NPEs by checking for invalid block/chunk access

    @Override
    public @Nullable Node getValidStart(@NotNull MobContext mobContext,
                                        @NotNull Point target) {
        final Point startPoint = mobContext.startPoint();

        // check if the mob has clearance to spawn at the starting point
        // this will typically fail if the mob is stuck in a block
        if (!canStand(mobContext, startPoint)) {
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

        final Block oldBlock = mobContext.instance().getBlock(oldPoint, Block.Getter.Condition.TYPE);
        final Block newBlock = mobContext.instance().getBlock(newPoint, Block.Getter.Condition.TYPE);
        final Block belowNewBlock = mobContext.instance().getBlock(belowNewPoint, Block.Getter.Condition.TYPE);

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
            if (!canStand(mobContext, evaluationContext.oldPoint().add(direction.blockX(), 0, 0))
                    || !canStand(mobContext, evaluationContext.oldPoint().add(0, 0, direction.blockZ()))) {
                // TODO: test if we can jump diagonally instead of failing?
                return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
            }
        }

        // check if they have the clearance needed to actually fit in this position without any obstructions
        if (!canStand(mobContext, evaluationContext.newPoint())) {
            // since they can't fit at the new spot, we'll check to see if they can step or jump to get in a position where they will fit
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
        return checkFallMoveForLargeBoundingBox(evaluationContext);
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

        return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE, fallNode);
    }

    @NotNull
    private NodeEvaluationResult checkFallMoveForLargeBoundingBox(@NotNull EvaluationContext evaluationContext) {
        // check if the blocks up to the max safe fall distance are air
        // we'll have to approach this a bit differently for larger bounding boxes
        // this will be a bit more computationally expensive...
        final BoundingBox boundingBox = evaluationContext.boundingBox();

        final Point oldPoint = evaluationContext.oldPoint();
        final Point newPoint = evaluationContext.newPoint();

        // TODO: depending on the scale of the mob, I might need to loop around the old or new point and check if it supports the mob standing here?
        // TODO: only having 2 points next to each other wont work for very large mobs

        double landingY = Double.MAX_VALUE;
        for (int y = evaluationContext.newPoint().blockY(); y >= evaluationContext.belowNewPoint().blockY() - evaluationContext.mobContext().safeFallDistance(); y--) {
            if (!canStand(evaluationContext.mobContext(), evaluationContext.newPoint().withY(y))) {
                // if the entity can't stand here, then it can't fall here...
                return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE);
            }
//            final BoundingBox.PointIterator blockIterator = boundingBox.getBlocks(evaluationContext.newPoint().withY(y));
//            while (blockIterator.hasNext()) {
//                final MutableVec blockPoint = blockIterator.next();
//                final Block block = evaluationContext.instance().getBlock(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ(), Block.Getter.Condition.TYPE);
//                if (block != null && !block.isAir()) {
//
//                }
//            }
        }

        return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
    }

    @NotNull
    private NodeEvaluationResult checkUpwardsMove(@NotNull EvaluationContext evaluationContext) {
        final Shape oldBlockShape = evaluationContext.oldBlockShape();

        // gets the 2 blocks in front of the entity
        // since the jump height is 1.25, we need to be able to see if it's a block with a slab or a block with powdered snow, etc...
        final Shape newBlockShape = evaluationContext.newBlockShape();
        final Shape aboveNewBlockShape = evaluationContext.instance().getBlock(evaluationContext.newPoint().add(0, 1, 0)).registry().collisionShape();

        final double totalBlockHeight = (newBlockShape.relativeEnd().y() + aboveNewBlockShape.relativeEnd().y()) - oldBlockShape.relativeEnd().y();
        System.out.println("Total Block Height: " + totalBlockHeight);

        if (totalBlockHeight > 0.0D && totalBlockHeight <= MAXIMUM_JUMP_HEIGHT) {
            System.out.println("Can step/jump");

            // check the clearance at the old point and the new point with the modified Y value from the step/jump
            if (!canStand(evaluationContext.mobContext(), evaluationContext.oldPoint().add(0, totalBlockHeight, 0))
                    || !canStand(evaluationContext.mobContext(), evaluationContext.newPoint().add(0, totalBlockHeight, 0))) {
                return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
            }

            final Node upwardsNode = new Node(
                    evaluationContext.newPoint().add(0, totalBlockHeight, 0),
                    evaluationContext.newNode().start(),
                    evaluationContext.newNode().target(),
                    evaluationContext.newNode().depth() + 1
            );
            upwardsNode.setType(totalBlockHeight <= MAXIMUM_STEP_HEIGHT ? Node.Type.STEP : Node.Type.JUMP);

            return new NodeEvaluationResult(NodeEvaluationResult.Status.VALID_MOVE, upwardsNode);
        }

        return new NodeEvaluationResult(NodeEvaluationResult.Status.INVALID_MOVE);
    }

    private boolean canStand(@NotNull MobContext mobContext,
                             @NotNull Point point) {
        final BoundingBox boundingBox = mobContext.boundingBox();
        final BoundingBox.PointIterator blockIterator = boundingBox.getBlocks(point);
        while (blockIterator.hasNext()) {
            final MutableVec blockPoint = blockIterator.next();
            final Block block = mobContext.instance().getBlock(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ(), Block.Getter.Condition.TYPE);

            if (block == null) continue;
            if (block.id() == Block.SCAFFOLDING.id()) continue;

            final boolean hit = block.registry().collisionShape().intersectBox(
                    point.sub(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ()),
                    boundingBox
            );
            if (hit) {
                return false;
            }
        }

        return true;
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