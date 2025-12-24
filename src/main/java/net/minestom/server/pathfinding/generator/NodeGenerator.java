package net.minestom.server.pathfinding.generator;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.NotNull;

public abstract class NodeGenerator {

    protected static final double MOB_JUMP_HEIGHT = 1.25;

    public abstract Node[] getValidNeighbors(@NotNull Node node,
                                             @NotNull Block.Getter context,
                                             @NotNull BoundingBox boundingBox);
}