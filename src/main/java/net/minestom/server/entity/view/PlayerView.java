package net.minestom.server.entity.view;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;

/**
 * Keeps track of a {@link Player}'s visible entities and viewer rule.
 * <br>
 * <b>Not thread-safe, only use and access within {@link ViewEngine}.</b>
 */
@ApiStatus.Internal
public final class PlayerView {

    private final Player player;

    private final IntSet visibleEntities;

    private volatile Predicate<Entity> viewerRule;
    private volatile boolean autoViewEntities;

    public PlayerView(Player player) {
        this.player = player;

        this.visibleEntities = new IntOpenHashSet();

        this.viewerRule = _ -> true;
        this.autoViewEntities = true;
    }

    /**
     * Gets the entity IDs of the current visible entities for this player.
     *
     * @return the set of entity IDs of the current visible entities
     */
    public IntSet getVisibleEntities() {
        return visibleEntities;
    }

    /**
     * Gets the rule required for {@link Entity}s to satisfy in order for this player to view them.
     * <br>
     * Default value is this player can see all entities.
     *
     * @return the viewer rule predicate
     */
    public Predicate<Entity> getViewerRule() {
        return viewerRule;
    }

    /**
     * Sets the rule required for {@link Entity}s to satisfy in order for this player to view them.
     * <br>
     * <b>If you simply want to hide or show all entities for this player, use the hideEntities() and showEntities() functions
     * respectively.</b>
     *
     * @param viewerRule the new viewer rule predicate
     */
    public void setViewerRule(Predicate<Entity> viewerRule) {
        this.viewerRule = viewerRule;
    }

    /**
     * Determines if the player can auto-view entities (all entities will be visible by default) or not
     * (no entities will be visible by default).
     * <br>
     * Default value is true.
     *
     * @return true if the player can auto-view entities, false if not
     */
    public boolean isAutoViewEntities() {
        return autoViewEntities;
    }

    /**
     * Sets whether the player can auto-view entities or not.
     * <br>
     * <b>Do not use this method unless you know what you are doing, setting this value to true will allow your
     * viewer rule to be skipped over, regardless of the result of the viewer predicate.</b>
     *
     * @param autoViewEntities true if the player can auto-view entities, false if not
     */
    public void setAutoViewEntities(boolean autoViewEntities) {
        this.autoViewEntities = autoViewEntities;
    }

    /**
     * Gets the player that this player view represents.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }
}