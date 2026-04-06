package net.minestom.server.entity.view;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

import java.util.function.Predicate;

// keeps track of an entity's viewers
// not thread-safe, only use within ViewEngine
public final class EntityView {

    private final Entity entity;

    private final IntSet viewers;

    private volatile Predicate<Player> viewableRule;
    private volatile boolean autoViewable;

    public EntityView(Entity entity) {
        this.entity = entity;

        this.viewers = new IntOpenHashSet();

        this.viewableRule = _ -> true;
        this.autoViewable = true;
    }

    public IntSet getViewers() {
        return viewers;
    }

    public Predicate<Player> getViewableRule() {
        return viewableRule;
    }

    public void setViewableRule(Predicate<Player> viewableRule) {
        this.viewableRule = viewableRule;
    }

    public boolean isAutoViewable() {
        return autoViewable;
    }

    public void setAutoViewable(boolean autoViewable) {
        this.autoViewable = autoViewable;
    }

    public Entity getEntity() {
        return entity;
    }
}