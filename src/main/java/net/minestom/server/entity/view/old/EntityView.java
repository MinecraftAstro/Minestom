//package net.minestom.server.entity.view.old;
//
//import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
//import it.unimi.dsi.fastutil.ints.IntSet;
//import net.minestom.server.entity.Entity;
//import net.minestom.server.entity.Player;
//import net.minestom.server.instance.EntityTracker;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.*;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
//final class EntityView {
//
//    private final Entity owningEntity;
//    private final Set<Player> manualViewers = new HashSet<>();
//    private final Object mutex = this;
//
//    // decides which players should be able to see the owning entity
//    public final Option<Player> viewableOption;
//
//    // decides which entities the owning entity should see
//    // this is only meaningful when the owning entity is a player
//    public final Option<Entity> viewerOption;
//
//    public EntityView(Entity owningEntity) {
//        this.owningEntity = owningEntity;
//
//        this.viewableOption = new Option<>(
//                EntityTracker.Target.PLAYERS,
//                Entity::autoViewEntities,
//                player -> showTo(player),
//                player -> hideFrom(player)
//        );
//
//        this.viewerOption = new Option<>(
//                EntityTracker.Target.ENTITIES,
//                Entity::isAutoViewable,
//                createViewerAdditionAction(),
//                createViewerRemovalAction()
//        );
//    }
//
//    @Nullable
//    private Consumer<Entity> createViewerAdditionAction() {
//        if (!(owningEntity instanceof Player player))
//            return null;
//
//        return entity -> entity.viewEngine.viewableOption.addition.accept(player);
//    }
//
//    @Nullable
//    private Consumer<Entity> createViewerRemovalAction() {
//        if (!(owningEntity instanceof Player player))
//            return null;
//
//        return entity -> entity.viewEngine.viewableOption.removal.accept(player);
//    }
//
//    public void hideFrom(Player player) {
//        // TODO: we shouldnt be able to hide a vehicle that we're riding (or any vehicle beneath the vehicle we're riding)
//
//        // lock the two entity views in a consistent order to prevent deadlocks
//        // the lowest entity ID will be the first lock and the higher entity ID will be the second lock
//        final Entity firstLock = player.getEntityId() < owningEntity.getEntityId() ? player : owningEntity;
//        final Entity secondLock = firstLock == owningEntity ? player : owningEntity;
//
//        synchronized (firstLock.viewEngine.mutex) {
//            synchronized (secondLock.viewEngine.mutex) {
//                // register the player for the entity as someone who will not be able to see it
//                owningEntity.viewEngine.viewableOption.unregister(player);
//
//                // register the entity from the player as something they can't see
//                player.viewEngine.viewerOption.unregister(owningEntity);
//            }
//        }
//
//        // remove the entity for the player
//        player.sendPackets(owningEntity.getRemovedViewerPackets());
//
//        // remove any passengers of this entity as well
//        for (Entity passenger : owningEntity.getPassengers()) {
//            System.out.println("removing passenger: " + passenger.getEntityType().name());
//            passenger.viewEngine.hideFrom(player);
//        }
//    }
//
//    /**
//     * A recursive function that collects
//     *
//     * @param player
//     * @param visibilityChain
//     */
//    private static void collectVisibleEntityChain(Entity entity,
//                                                  Player player,
//                                                  List<Entity> visibilityChain) {
//        // lock the two entity views in a consistent ordering to prevent deadlocks
//        // the lowest entity ID will be the first lock and the higher entity ID will be the second lock
//        final Entity firstLock = player.getEntityId() < entity.getEntityId() ? player : entity;
//        final Entity secondLock = firstLock == entity ? player : entity;
//
//        boolean shouldBeViewing = false;
//        synchronized (firstLock.viewEngine.mutex) {
//            synchronized (secondLock.viewEngine.mutex) {
//                if (shouldBeViewing(entity, player)) {
//                    System.out.println("should be viewing: " + entity.getEntityType().name());
//                    // register the player for the entity as someone who can see it
//                    entity.viewEngine.viewableOption.register(player);
//
//                    // register the entity as something the player can see
//                    player.viewEngine.viewerOption.register(entity);
//
//                    shouldBeViewing = true;
//                }
//            }
//        }
//
//        // this entity should be visible to the player
//        if (shouldBeViewing) {
//            visibilityChain.add(entity);
//
//            // check if this entity's passenger(s) (and their passenger(s)) are visible to the player
//            // this fixes an issue where passengers are not properly synced when their visibility status is updated
//            for (Entity passenger : entity.getPassengers()) {
//                collectVisibleEntityChain(passenger, player, visibilityChain);
//            }
//        }
//    }
//
//    private static boolean shouldBeViewing(Entity entity, Player player) {
//        // check if the player can already view the entity
//        if (entity.hasViewer(player))
//            return false;
//
//        // check if the player is riding the entity
//        // if so, then they can already view the entity
//        if (player.getVehicle() == entity)
//            return false;
//
//        // make sure that the entity's vehicle chain is visible (if they have one)
//        // if the entity's vehicle chain is not visible, then the entity (and any of its passenger(s)) will not be visible either
//        Entity vehicle = entity.getVehicle();
//        while (vehicle != null) {
//            if (!vehicle.viewEngine.viewableOption.predicate(player) || !player.viewEngine.viewerOption.predicate(vehicle))
//                return false;
//
//            vehicle = vehicle.getVehicle();
//        }
//
//        // check if the entity registers the player as someone who can see it
//        // if not, then this won't be a visible entity
//        if (!entity.viewEngine.viewableOption.predicate(player)
//                || (!entity.viewEngine.viewableOption.isAuto() && !entity.viewEngine.manualViewers.contains(player)))
//            return false;
//
//        // check if the player registers the entity as something it can see
//        // if not, then this won't be a visible entity
//        if (!player.viewEngine.viewerOption.predicate(entity)
//                || (!player.viewEngine.viewerOption.isAuto() && !player.viewEngine.manualViewers.contains(player)))
//            return false;
//
//        return true;
//    }
//
//    public boolean manualAdd(Player player) {
//        if (player == this.owningEntity)
//            return false;
//
//        boolean added;
//        synchronized (mutex) {
//            added = manualViewers.add(player);
////            if (manualViewers.add(player)) {
////                viewableOption.bitSet.add(player.getEntityId());
////                return true;
////            }
////
////            return false;
//        }
//
//        if (added) {
//            showTo(player);
//        }
//
//        return added;
//    }
//
//    public boolean manualRemove(Player player) {
//        if (player == this.owningEntity) return false;
//
//        boolean removed;
//        synchronized (mutex) {
//            removed = manualViewers.remove(player);
//        }
//
//        if (removed) {
//            hideFrom(player);
//        }
//
//        return removed;
//    }
//
//    public void forManuals(Consumer<Player> consumer) {
//        synchronized (mutex) {
//            Set<Player> manualViewersCopy = Set.copyOf(this.manualViewers);
//            manualViewersCopy.forEach(consumer);
//        }
//    }
//
////    public boolean hasPredictableViewers() {
////        // Verify if this entity's viewers can be predicted from surrounding entities
////        synchronized (mutex) {
////            return viewableOption.isAuto() && viewableOption.predicate == null && manualViewers.isEmpty();
////        }
////    }
//
//    public void handleAutoViewAddition(Entity entity) {
//        handleAutoView(entity, viewerOption.addition, viewableOption.addition);
//    }
//
//    public void handleAutoViewRemoval(Entity entity) {
//        handleAutoView(entity, viewerOption.removal, viewableOption.removal);
//    }
//
//    private void handleAutoView(Entity entity, Consumer<Entity> viewer, Consumer<Player> viewable) {
//        // if this view engine is a player
//        // and the viewer option
//        if (this.owningEntity instanceof Player && viewerOption.isAuto() && entity.isAutoViewable()) {
//            if (viewer != null) viewer.accept(entity); // Send packet to this player
//        }
//
//        if (entity instanceof Player player && player.autoViewEntities() && viewableOption.isAuto()) {
//            if (viewable != null) viewable.accept(player); // Send packet to the range-visible player
//        }
//    }
//
//    public final class Option<T extends Entity> {
//
//        // the type of entities that should be tracked by this option
//        private final EntityTracker.Target<T> target;
//        // The condition that must be met for this option to be considered auto.
//        private final Predicate<T> loopPredicate;
//        // The consumers to be called when an entity is added/removed.
//        @Nullable
//        public final Consumer<T> addition, removal;
//        // Contains all the auto-entity ids that are viewable by this option.
//        public final IntSet bitSet = new IntOpenHashSet();
//        // 1 if auto, 0 if manual
//        private volatile int auto = 1;
//        // The custom rule used to determine if an entity is viewable.
//        // null if auto-viewable
//        @Nullable
//        private Predicate<T> predicate;
//
//        public Option(EntityTracker.Target<T> target,
//                      Predicate<T> loopPredicate,
//                      @Nullable Consumer<T> addition,
//                      @Nullable Consumer<T> removal) {
//            this.target = target;
//            this.loopPredicate = loopPredicate;
//            this.addition = addition;
//            this.removal = removal;
//        }
//
//        public boolean isAuto() {
//            return auto == 1;
//        }
//
//        public boolean predicate(T entity) {
//            final Predicate<T> predicate = this.predicate;
//            return predicate == null || predicate.test(entity);
//        }
//
//        public boolean isRegistered(T entity) {
//            return bitSet.contains(entity.getEntityId());
//        }
//
//        public void register(T entity) {
//            assert entity.getInstance() != null : "Instance-less entity shouldn't be registered as viewer";
//            this.bitSet.add(entity.getEntityId());
//        }
//
//        public void unregister(T entity) {
//            this.bitSet.remove(entity.getEntityId());
//        }
//
//        public void updateAuto(boolean autoViewable) {
//            final boolean previous = UPDATER.getAndSet(this, autoViewable ? 1 : 0) == 1;
//
//            // make sure that the previous value is not equal to the new value (autoViewable)
//            // if it is, then we don't have to worry about any view updates
//            if (previous == autoViewable)
//                return;
//
//            synchronized (mutex) {
//                if (autoViewable) {
//                    System.out.println("Add all potential viewers");
//                    update(loopPredicate, addition);
//                } else {
//                    System.out.println("Remove all viewers");
//                    update(this::isRegistered, removal);
//                }
//            }
//        }
//
//        public void updateRule(Predicate<T> predicate) {
//            synchronized (mutex) {
//                this.predicate = predicate;
//                updateRule0(predicate);
//            }
//        }
//
//        public void updateRule() {
//            synchronized (mutex) {
//                updateRule0(predicate);
//            }
//        }
//
//        void updateRule0(Predicate<T> predicate) {
//            if (predicate == null) {
//                update(loopPredicate, entity -> {
//                    if (!isRegistered(entity)) addition.accept(entity);
//                });
//            } else {
//                update(loopPredicate, entity -> {
//                    final boolean result = predicate.test(entity);
//                    if (result != isRegistered(entity)) {
//                        if (result) addition.accept(entity);
//                        else removal.accept(entity);
//                    }
//                });
//            }
//        }
//
//        private void update(Predicate<T> visibilityPredicate,
//                            Consumer<T> action) {
//            for (T entity : references()) {
//                // skip over self or invisible entities
//                if (entity == EntityView.this.owningEntity || !visibilityPredicate.test(entity))
//                    continue;
//
//                // skip over manual viewers
//                if (entity instanceof Player player && manualViewers.contains(player))
//                    continue;
//
//                // skip over passengers
//                if (entity.getVehicle() != null)
//                    continue;
//
//                action.accept(entity);
//            }
//        }
//    }
//}