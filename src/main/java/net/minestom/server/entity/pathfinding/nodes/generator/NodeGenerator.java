package net.minestom.server.entity.pathfinding.nodes.generator;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.pathfinding.nodes.Node;
import net.minestom.server.entity.pathfinding.path.PathWalker;
import net.minestom.server.instance.BlockBatch;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class NodeGenerator {

    private static final double JUMP_HEIGHT = 1.2522D;
    private static final int CEIL_JUMP_HEIGHT = 2;

    private static final double STEP_HEIGHT = 0.6D;
    private static final int CEIL_STEP_HEIGHT = 1;

    @NotNull
    public Collection<Node> generateNodes(@NotNull PathWalker pathWalker,
                                          @NotNull BlockBatch blockBatch,
                                          @NotNull Node currentNode,
                                          @NotNull Point endPoint) {
        final Point point = currentNode.point();

        final double height = pathWalker.getBoundingBox().height();
        //final int ceilHeight = (int) Math.ceil(height);

        final int stepSize = Math.max((int) pathWalker.getBoundingBox().width() / 2, 1);

        // 950ms
        final Collection<Node> nodes = new ArrayList<>(24);

        for (int x = -stepSize; x <= stepSize; x++) {
            for (int z = -stepSize; z <= stepSize; z++) {
                // ignore the position that the entity is standing on
                if (x == 0 && z == 0) continue;

                final double cost = Math.sqrt(x * x + z * z) * 1.5;

                int blockX = point.blockX() + x;
                int blockY = point.blockY();
                int blockZ = point.blockZ() + z;
                final Point blockPoint = new Vec(blockX, blockY, blockZ);

                final Block block = blockBatch.getBlock(blockX, blockY, blockZ, Block.Getter.Condition.TYPE);
                if (block.isAir()) {
                    // check for walking or falling
                    final Node walkNode = new Node(Node.Type.WALK, currentNode.g() + cost, blockPoint.distance(endPoint), blockPoint, currentNode);
                    nodes.add(walkNode);
                } else {
                    // check for walking, stepping, or jumping
                    /*double clearanceNeeded = 0.0D;
                    for (int y = 0; y < CEIL_JUMP_HEIGHT; y++) {
                        final Block elevatedBlock = blockBatch.getBlock(blockX, blockY + y, blockZ, Block.Getter.Condition.TYPE);
                        final Shape shape = elevatedBlock.registry().collisionShape();
                        final double difference = shape.relativeEnd().y() - shape.relativeStart().y();
                        clearanceNeeded += difference;
                    }

                    System.out.println("Clearance Needed: " + clearanceNeeded);
                    if(JUMP_HEIGHT >= clearanceNeeded) {
                        System.out.println("Added jump node");
                        final Node jumpNode = new Node(Node.Type.JUMP, currentNode.g() + cost, currentNode.point().distance(endPoint), new Vec(blockX, blockY + clearanceNeeded, blockZ), currentNode);
                        nodes.add(jumpNode);
                    }*/
                }
            }
        }

        return nodes;
    }

    /*private double stepHeight(@NotNull LivingEntity entity) {
        return entity.getAttributeValue(Attribute.STEP_HEIGHT);
    }

    private double jumpStrength(@NotNull LivingEntity entity) {
        return entity.getAttributeValue(Attribute.JUMP_STRENGTH);
    }

    // https://www.mcpk.wiki/wiki/Vertical_Movement_Formulas
    // https://www.mcpk.wiki/wiki/Jumping
    private double jumpHeight(@NotNull LivingEntity entity) {
        final int jumpBoostLevel = entity.getEffectLevel(PotionEffect.JUMP_BOOST);
        final double jumpStrength = jumpStrength(entity);

        // no need to do unnecessary computations if we already have the correct jump height
        // check if we need to cache or re-cache the jump height (jump boost level change, jump strength change, etc)
        if (cachedJumpHeight == Double.MIN_VALUE
                || Double.compare(cachedJumpStrength, jumpStrength) != 0
                //|| !MathUtils.isEqual(cachedJumpStrength, jumpStrength, 0.001)
                || cachedJumpBoostLevel != jumpBoostLevel) {
            System.out.println("Calculating jump height");
            double velocity = jumpStrength + ((jumpBoostLevel == -1) ? 0 : (jumpBoostLevel * 0.1));
            double height = 0.0D;
            while (velocity >= 0.003D) {
                height += velocity;

                // apply the gravity and drag every tick
                velocity = (velocity - 0.08) * 0.98;

                cachedJumpStrength = jumpStrength;
                cachedJumpBoostLevel = jumpBoostLevel;

                cachedJumpHeight = height;
            }
        }

        return cachedJumpHeight;
    }

    private double safeFallDistance(@NotNull LivingEntity entity) {
        return entity.getAttributeValue(Attribute.SAFE_FALL_DISTANCE);
    }*/

    /*private long encodeKey(long chunkIndex, int blockIndex) {
        return (chunkIndex << 32) | (blockIndex & 0xFFFFFFFFL);
    }*/
}