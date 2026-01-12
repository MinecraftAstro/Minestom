package net.minestom.server.pathfinding.context;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.NotNull;

public final class ValidationContext {

    private final MobContext mobContext;

    private final Node oldNode;
    private final Node newNode;

    private final Point oldPoint;
    private final Point newPoint;
    private final Point belowNewPoint;

    private final Block oldBlock;
    private final Block newBlock;
    private final Block belowNewBlock;

    private final Shape oldBlockShape;
    private final Shape newBlockShape;
    private final Shape belowNewBlockShape;

    public ValidationContext(@NotNull MobContext mobContext,
                             @NotNull Node oldNode,
                             @NotNull Node newNode,
                             @NotNull Point oldPoint,
                             @NotNull Point newPoint,
                             @NotNull Point belowNewPoint,
                             @NotNull Block oldBlock,
                             @NotNull Block newBlock,
                             @NotNull Block belowNewBlock,
                             @NotNull Shape oldBlockShape,
                             @NotNull Shape newBlockShape,
                             @NotNull Shape belowNewBlockShape) {
        this.mobContext = mobContext;
        this.oldNode = oldNode;
        this.newNode = newNode;
        this.oldPoint = oldPoint;
        this.newPoint = newPoint;
        this.belowNewPoint = belowNewPoint;
        this.oldBlock = oldBlock;
        this.newBlock = newBlock;
        this.belowNewBlock = belowNewBlock;
        this.oldBlockShape = oldBlockShape;
        this.newBlockShape = newBlockShape;
        this.belowNewBlockShape = belowNewBlockShape;
    }

    public MobContext mobContext() {
        return mobContext;
    }

    public Instance instance() {
        return mobContext.instance();
    }

    public BoundingBox boundingBox() {
        return mobContext.boundingBox();
    }

    public Node oldNode() {
        return oldNode;
    }

    public Node newNode() {
        return newNode;
    }

    public Point oldPoint() {
        return oldPoint;
    }

    public Point newPoint() {
        return newPoint;
    }

    public Point belowNewPoint() {
        return belowNewPoint;
    }

    public Block oldBlock() {
        return oldBlock;
    }

    public Block newBlock() {
        return newBlock;
    }

    public Block belowNewBlock() {
        return belowNewBlock;
    }

    public Shape oldBlockShape() {
        return oldBlockShape;
    }

    public Shape newBlockShape() {
        return newBlockShape;
    }

    public Shape belowNewBlockShape() {
        return belowNewBlockShape;
    }
}