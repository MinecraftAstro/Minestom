package net.minestom.server.pathfinding.data;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minestom.server.coordinate.Point;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import org.jetbrains.annotations.NotNull;

/**
 * The GridRegionData class represents the data associated with a grid region. This data includes a
 * Bloom filter used to quickly check if a position is within the region and a set of positions that
 * have been examined by the pathfinder.
 */
public final class GridRegionData {

    /**
     * The Bloom filter used to store the positions of the region. This filter is used to quickly
     * check if a position is within the region without having to iterate over all the positions in
     * the region.
     */
    private final BloomFilter<Point> bloomFilter;

    /**
     * The set of positions that have been examined by the pathfinder. This set is used to track the
     * positions that have been examined by the pathfinder to avoid examining the same position
     * multiple times.
     */
    private final LongSet regionalExaminedPositions;

    /**
     * Creates a new GridRegionData with Bloom filter settings from the provided configuration.
     *
     * @param options The pathfinder options containing Bloom filter settings
     */
    public GridRegionData(@NotNull PathfinderOptions options) {
        final Funnel<Point> pointFunnel = (point, into) ->
                into.putInt(point.blockX()).putInt(point.blockY()).putInt(point.blockZ());

        this.bloomFilter = BloomFilter.create(pointFunnel, options.bloomFilterSize(), options.bloomFilterFpp());
        this.regionalExaminedPositions = new LongOpenHashSet();
    }

    @NotNull
    public BloomFilter<Point> getBloomFilter() {
        return bloomFilter;
    }

    @NotNull
    public LongSet getRegionalExaminedPositions() {
        return regionalExaminedPositions;
    }
}