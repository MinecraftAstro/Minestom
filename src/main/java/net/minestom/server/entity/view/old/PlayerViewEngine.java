//package net.minestom.server.entity.view.old;
//
//import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
//import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
//import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
//import it.unimi.dsi.fastutil.ints.IntSet;
//import net.minestom.server.ServerFlag;
//import net.minestom.server.coordinate.Point;
//import net.minestom.server.entity.Entity;
//import net.minestom.server.entity.Player;
//import net.minestom.server.instance.EntityTracker;
//import net.minestom.server.instance.Instance;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.function.Predicate;
//
//public final class PlayerViewEngine {
//
//    // useful to avoid casting on this.entity everytime we need the player
//    private final Player player;
//
//    // stores the current visible entity IDs
//    private final IntSet currentVisibleEntities = new IntOpenHashSet();
//
//    // this rule is used to determine if this player can view an entity, by default this player can view all viewable entities
//    Predicate<Entity> viewerRule = _ -> true;
//    volatile boolean autoViewEntities = true;
//
//    public PlayerViewEngine(Player player) {
//        super(player);
//
//        this.player = player;
//    }
//
//    public void hideEntities() {
//        synchronized (mutex) {
//            viewerRule = _ -> false;
//            autoViewEntities = false;
//
//            updateViewerRule(viewerRule);
//        }
//    }
//
//    public void showEntities() {
//        synchronized (mutex) {
//            viewerRule = _ -> true;
//            autoViewEntities = true;
//
//            updateViewerRule(viewerRule);
//        }
//    }
//
//    public void setViewerRule(Predicate<Entity> newViewerRule) {
//        synchronized (mutex) {
//            viewerRule = newViewerRule;
//            autoViewEntities = false;
//
//            updateViewerRule(newViewerRule);
//        }
//    }
//
//    private void updateViewerRule(Predicate<Entity> newViewerRule) {
//        if (autoViewEntities) {
//            // if the new viewer rule allows this player to view all entities then we only have to
//            // check if the entity allows the player to view them
//            for (Entity entity : getNearbyEntities()) {
//                // skip over ourselves, we are always visible to ourselves...
//                if (player == entity)
//                    continue;
//
//                // skip over passengers, we only really care about the root entity
//
//                // check if this player can already view the entity
//                if (entity.hasViewer(player))
//                    continue;
//
//                // if the entity is auto-viewable, then we know this player can be a viewer
//                if (entity.viewEngine.autoViewable) {
//                    // TODO: accept this player
//                }
//
//                // since the entity is not auto-viewable, we'll have to check if this player can be a viewer
//                if (entity.viewEngine.viewableRule.test(player)) {
//                    // TODO: accept this player
//                }
//            }
//        } else {
//            // the new viewer rule potentially does not allow this player to view certain entities
//            // we'll have to check if an entity is visible to this player and if this player can view an entity
//            for (Entity entity : getNearbyEntities()) {
//                // skip over ourselves, we are always visible to ourselves...
//                if (this.entity == entity)
//                    continue;
//            }
//        }
//    }
//}