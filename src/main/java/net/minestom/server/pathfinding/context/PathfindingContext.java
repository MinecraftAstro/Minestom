package net.minestom.server.pathfinding.context;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minestom.server.pathfinding.collections.BinaryMinimumHeap;
import net.minestom.server.pathfinding.data.GridRegionData;
import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.NotNull;

public final class PathfindingContext {

    private final BinaryMinimumHeap openSet;
    private final Long2ObjectMap<GridRegionData> visitedRegions;
    private final Long2ObjectMap<Node> openSetNodes;

    public PathfindingContext(@NotNull BinaryMinimumHeap openSet,
                              Long2ObjectMap<GridRegionData> visitedRegions,
                              Long2ObjectMap<Node> openSetNodes) {
        this.openSet = openSet;
        this.visitedRegions = visitedRegions;
        this.openSetNodes = openSetNodes;
    }

    public BinaryMinimumHeap openSet() {
        return openSet;
    }

    public Long2ObjectMap<GridRegionData> visitedRegions() {
        return visitedRegions;
    }

    public Long2ObjectMap<Node> openSetNodes() {
        return openSetNodes;
    }
}