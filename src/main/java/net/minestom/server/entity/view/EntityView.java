package net.minestom.server.entity.view;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Keeps track of a {@link Entity}'s viewers and viewable rule.
 * <br>
 * <b>Not thread-safe, only use and access within {@link ViewEngine}.</b>
 */
@ApiStatus.Internal
public final class EntityView {

    private final IntSet viewers;
    private final Set<Player> manualViewers;

    private volatile Predicate<Player> viewableRule;
    private volatile boolean autoViewable;

    public EntityView() {
        this.viewers = new IntOpenHashSet();
        this.manualViewers = new HashSet<>();

        this.viewableRule = _ -> true;
        this.autoViewable = true;
    }

    /**
     * Gets the entity IDs of the current viewers for this entity.
     *
     * @return the set of entity IDs of the current viewers
     */
    public IntSet getViewers() {
        return viewers;
    }

    /**
     * Gets the manual viewers for this entity.
     * <br>
     * Manual viewers will always be able to see this entity (unless it's hidden due to it being a passenger of a hidden entity) until
     * they are removed from this collection.
     *
     * @return the set of manual viewers
     */
    public Set<Player> getManualViewers() {
        return manualViewers;
    }

    /**
     * Gets the rule required for {@link Player}s to satisfy in order to view this entity.
     * <br>
     * Default value is this entity can be seen by all players.
     *
     * @return the viewable rule predicate
     */
    public Predicate<Player> getViewableRule() {
        return viewableRule;
    }

    /**
     * Sets the rule required for {@link Player}s to satisfy in order to view this entity.
     * <br>
     * <b>If you simply want to hide or show this entity to all players, use the hide() and show() functions
     * respectively, this is especially important for showing this entity to all players since it allows us
     * to potentially optimize packet sending (due to having predictable viewers).</b>
     *
     * @param viewableRule the new viewable rule predicate
     */
    public void setViewableRule(Predicate<Player> viewableRule) {
        this.viewableRule = viewableRule;
    }

    /**
     * Determines if the entity is auto-viewable (all players will be able to see it by default) or not
     * (no players will be able to see it by default).
     * <br>
     * Default value is true.
     *
     * @return true if the entity is auto-viewable, false if not
     */
    public boolean isAutoViewable() {
        return autoViewable;
    }

    /**
     * Sets whether the entity is auto-viewable or not.
     * <br>
     * <b>Do not use this method unless you know what you are doing, setting this value to true potentially allows
     * packets to be optimized when sent to viewers, this can give incorrect results if this value is wrong.</b>
     *
     * @param autoViewable true if the entity is auto-viewable, false if not
     */
    public void setAutoViewable(boolean autoViewable) {
        this.autoViewable = autoViewable;
    }
}