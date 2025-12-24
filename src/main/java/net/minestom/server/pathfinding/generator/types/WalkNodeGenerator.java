package net.minestom.server.pathfinding.generator.types;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.Shape;
import net.minestom.server.collision.SweepResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.generator.NodeGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WalkNodeGenerator extends NodeGenerator {

    @Override
    public Node[] getValidNeighbors(@NotNull Node node,
                                    @NotNull Block.Getter context,
                                    @NotNull BoundingBox boundingBox) {
        //final int stepSize = (int) Math.max(Math.floor(boundingBox.width() / 2), 1);
        //final Node[] validNeighbors = new Node[Math.powExact(2 * stepSize + 1, 2)];
        final Node[] validNeighbors = new Node[9];

        final int x = node.x();
        final int y = node.y();
        final int z = node.z();

        // check to see if jumping is possible at the current node
        // if there is a block above their heads then they can't jump
        final int height = (int) Math.ceil(boundingBox.height());
        final boolean canJump = context.getBlock(x, y + height, z, Block.Getter.Condition.TYPE).isAir();
        System.out.println("Can Jump: " + canJump);

        for (int relativeX = -1; relativeX <= 1; relativeX++) {
            for (int relativeZ = -1; relativeZ <= 1; relativeZ++) {
                // this node has already been computed (it's the node passed in the method)
                if (relativeX == 0 && relativeZ == 0)
                    continue;

                // TODO: debug information, remove later
                //System.out.println(context.getBlock(x + relativeX, y, z + relativeZ, Block.Getter.Condition.TYPE));
                //System.out.println(context.getBlock(x + relativeX, y + 1, z + relativeZ, Block.Getter.Condition.TYPE));
                //System.out.println();

                /*
                Scenarios:

                If the blocks around
                 */

                final Node processedNode = processNode(x + relativeX, y, z + relativeZ, canJump);
            }
        }

        System.out.println("---------------------------");

        return validNeighbors;
    }

    @Nullable
    private Node processNode(int x, int y, int z, boolean canJump) {
        Node node = new Node(x, y, z);

        return node;
    }

    @Nullable
    private Node tryJump() {
        return null;
    }


    // TODO: check if it's a valid move (no obstacles)

    // TODO: check if it's a valid jump (if there is an obstacle)

    // TODO: check if they will fall, and if it's a safe fall
}