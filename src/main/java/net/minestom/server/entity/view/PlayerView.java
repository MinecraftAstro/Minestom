package net.minestom.server.entity.view;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

import java.util.function.Predicate;

// keeps track of a player's visible entities
// not thread-safe, only use within ViewEngine
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

    public IntSet getVisibleEntities() {
        return visibleEntities;
    }

    public Predicate<Entity> getViewerRule() {
        return viewerRule;
    }

    public void setViewerRule(Predicate<Entity> viewerRule) {
        this.viewerRule = viewerRule;
    }

    public boolean isAutoViewEntities() {
        return autoViewEntities;
    }

    public void setAutoViewEntities(boolean autoViewEntities) {
        this.autoViewEntities = autoViewEntities;
    }

    public Player getPlayer() {
        return player;
    }
}