package net.minestom.server.pathfinding.context;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minestom.server.pathfinding.collections.BinaryMinimumHeap;
import net.minestom.server.pathfinding.data.SpatialData;
import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.NotNull;

// handles the context of a pathfinding session
// this helps us from passing around a bunch of arguments when calling different pathfinding functions
public final class PathfindingContext {

    private final BinaryMinimumHeap openSet;
    private final Long2ObjectMap<SpatialData> visitedRegions;
    private final Long2ObjectMap<Node> openSetNodes;

    public PathfindingContext(@NotNull BinaryMinimumHeap openSet,
                              Long2ObjectMap<SpatialData> visitedRegions,
                              Long2ObjectMap<Node> openSetNodes) {
        this.openSet = openSet;
        this.visitedRegions = visitedRegions;
        this.openSetNodes = openSetNodes;
    }

    public BinaryMinimumHeap openSet() {
        return openSet;
    }

    public Long2ObjectMap<SpatialData> visitedRegions() {
        return visitedRegions;
    }

    public Long2ObjectMap<Node> openSetNodes() {
        return openSetNodes;
    }
}