//package net.minestom.server.entity.view.old;
//
//import it.unimi.dsi.fastutil.ints.*;
//import net.minestom.server.ServerFlag;
//import net.minestom.server.coordinate.Point;
//import net.minestom.server.entity.Entity;
//import net.minestom.server.entity.Player;
//import net.minestom.server.entity.view.PlayerViewUtils;
//import net.minestom.server.instance.EntityTracker;
//import net.minestom.server.instance.Instance;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.*;
//import java.util.function.Predicate;
//
//public final class EntityViewEngine {
//
//    protected final Entity entity;
//    protected final Object mutex = this;
//
//    private final IntSet currentViewers = new IntOpenHashSet();
//
//    // this rule is used to determine if an entity is viewable by a player, by default this entity is viewable by all players
//    Predicate<Player> viewableRule = _ -> true;
//    volatile boolean autoViewable = true;
//
//    // an unmodifiable set that holds the current viewers of this entity
//    final Set<Player> viewerSet = new ViewerSet();
//
//    @Nullable
//    protected volatile TrackedLocation trackedLocation;
//
//    public EntityViewEngine(Entity entity) {
//        this.entity = entity;
//    }
//
//    public void hide() {
//        synchronized (mutex) {
//            viewableRule = _ -> false;
//            autoViewable = false;
//
//            setViewableRuleInternal(viewableRule);
//        }
//    }
//
//    public void show() {
//        synchronized (mutex) {
//            viewableRule = _ -> true;
//            autoViewable = true;
//
//            setViewableRuleInternal(viewableRule);
//        }
//    }
//
//    public void setViewableRule(Predicate<Player> newViewableRule) {
//        synchronized (mutex) {
//            viewableRule = newViewableRule;
//            autoViewable = false;
//
//            setViewableRuleInternal(newViewableRule);
//        }
//    }
//
//    private void setViewableRuleInternal(Predicate<Player> newViewableRule) {
//        if(autoViewable) {
//
//        }
//
//        for (Player player : getNearbyPlayers()) {
//            // we won't be able to hide or show ourselves...
//            if (player == entity)
//                continue;
//        }
//    }
//
//    private int lastSize;
//
//    public boolean addViewer(Player player) {
//        if (player == entity)
//            return false;
//
//        if (player.getInstance() == null)
//            return false;
//
//        // check if the player can already view the entity
//        if (viewableRule.test(player)
//                && player.viewEngine.viewerRule.test(entity))
//            return false;
//
//        boolean canAdd = true;
//        synchronized (mutex) {
//            if (currentViewers.contains(player.getEntityId()))
//                canAdd = false;
//        }
//
//        if (canAdd)
//            PlayerViewUtils.showEntity(entity, player);
//
//        return canAdd;
//    }
//
//    public boolean removeViewer(Player player) {
//        if (player == entity)
//            return false;
//
//        synchronized (mutex) {
//            return currentViewers.remove(player.getEntityId());
//        }
//    }
//
//    /**
//     * Checks if this entity has the "default viewer rules", which means that it will be viewable
//     * by all players. This is useful information to have when sending viewable packets during
//     * position synchronization/updates.
//     *
//     * @return true if the entity is viewable to all players, false if not
//     */
//    public boolean hasPredictableViewers() {
//        return autoViewable;
//    }
//
//    // an entity has been added to the tracker which means they are in range or recently added
//    public void handleTrackerAddition(Entity entity) {
//        // this section is responsible for updating the visible entities for a player
//        if (this.entity instanceof Player player) {
//            if (player.viewEngine.isVisibleEntity(entity.getEntityId())) {
//                // do nothing since the entity is already visible for this player
//                System.out.println("Already visible entity for player: " + entity.getEntityType().name());
//            } else {
//                if (player.viewEngine.autoViewEntities && entity.viewEngine.autoViewable) {
//                    // the player can auto-view entities and the entity they are trying to view is auto-viewable
//                    // they should be able to view this entity
//                } else {
//                    // the player either can't auto-view entities or the entity is not auto-viewable
//                    // we'll have to run a few more tests to determine if they should view this entity
//                    if (entity.viewEngine.isManualViewer(player)) {
//                        // the player is a manual viewer of the entity
//                        // they should be able to view this entity
//                    } else if (player.viewEngine.viewerRule.test(entity)
//                            && entity.viewEngine.viewableRule.test(player)) {
//                        // the player can view this entity since they pass the respective rule checks
//                    }
//                }
//            }
//        }
//
//        // this section is responsible for updating the current viewers of an entity
//        if (entity instanceof Player player) {
//            // make sure that this entity does not have the player as a viewer already
//            if (isViewer(player) || isManualViewer(player)) {
//                System.out.println("Already a viewer of entity: " + this.entity.getEntityType().name());
//            } else {
//                if (player.viewEngine.autoViewEntities && autoViewable) {
//                    // the player can auto-view entities and this entity is auto-viewable
//                    // the entity should add this player as a viewer
//                    // TODO
//                } else {
//                    // the player either can't auto-view entities or this entity is not auto-viewable
//                    // we'll have to run a few more tests to determine if the player should be a viewer of this entity
//                    if (viewableRule.test(player)
//                            && player.viewEngine.viewerRule.test(entity)) {
//                        // the player passes the respective viewable and viewer rules
//                        // the entity should add this player as a viewer
//                        // TODO
//                    }
//                }
//            }
//        }
//    }
//
//    // an entity has been removed from the tracker which means they are out of range or recently removed
//    public void handleTrackerRemoval(Entity entity) {
//    }
//
//    /**
//     * Updates the current location (instance and point) of this entity.
//     * This is useful for viewing purposes because we want to keep track of nearby players and entities.
//     *
//     * @param instance the instance that this entity is in, or null if none
//     * @param point    the point that this entity is at
//     */
//    public void updateTracker(@Nullable Instance instance, Point point) {
//        this.trackedLocation = instance != null ? new TrackedLocation(instance, point) : null;
//    }
//
//    record TrackedLocation(Instance instance, Point point) {
//    }
//
//    /**
//     * An unmodifiable set that holds the current viewers of the {@code entity}.
//     */
//    final class ViewerSet extends AbstractSet<Player> {
//
//        @Override
//        public Iterator<Player> iterator() {
//            final List<Player> players;
//            synchronized (mutex) {
//                if (currentViewers.isEmpty())
//                    return Collections.emptyIterator();
//
//                final Instance instance = entity.getInstance();
//                if (instance == null)
//                    return Collections.emptyIterator();
//
//                players = new ArrayList<>(currentViewers.size());
//                for (IntIterator iterator = currentViewers.iterator(); iterator.hasNext(); ) {
//                    final int playerId = iterator.nextInt();
//                    final Player player = (Player) instance.getEntityById(playerId);
//                    if (player != null)
//                        players.add(player);
//                }
//            }
//
//            return players.iterator();
//        }
//
//        @Override
//        public int size() {
//            synchronized (mutex) {
//                final Instance instance = entity.getInstance();
//                if (instance == null)
//                    return 0;
//
//                int count = 0;
//                for (IntIterator iterator = currentViewers.iterator(); iterator.hasNext(); ) {
//                    final Entity viewer = instance.getEntityById(iterator.nextInt());
//                    if (viewer != null)
//                        count++;
//                }
//
//                return count;
//            }
//        }
//
//        @Override
//        public boolean isEmpty() {
//            synchronized (mutex) {
//                return currentViewers.isEmpty();
//            }
//        }
//
//        @Override
//        public boolean contains(Object object) {
//            // only objects of type player can be in this set
//            if (!(object instanceof Player player))
//                return false;
//
//            synchronized (mutex) {
//                return currentViewers.contains(player.getEntityId());
//            }
//        }
//    }
//}