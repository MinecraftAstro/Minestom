package net.minestom.server.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class EntityView {

    private final Entity owningEntity;
    private final Set<Player> manualViewers = new HashSet<>();
    private final Object mutex = this;

    // decides which players should be able to see the owning entity
    public final Option<Player> viewableOption;

    // decides which entities the owning entity should see
    // this is only meaningful when the owning entity is a player
    public final Option<Entity> viewerOption;

    // an unmodifiable set that holds the current viewers of the owning entity
    final Set<Player> viewerSet = new SetImpl();

    // the last tracked location of the owning entity, or null if there is none
    @Nullable
    private volatile TrackedLocation trackedLocation;

    public EntityView(Entity owningEntity) {
        this.owningEntity = owningEntity;

        this.viewableOption = new Option<>(
                EntityTracker.Target.PLAYERS,
                Entity::autoViewEntities,
                player -> showTo(player),
                player -> hideFrom(player)
        );

        this.viewerOption = new Option<>(
                EntityTracker.Target.ENTITIES,
                Entity::isAutoViewable,
                createViewerAdditionAction(),
                createViewerRemovalAction()
        );
    }

    @Nullable
    private Consumer<Entity> createViewerAdditionAction() {
        if (!(owningEntity instanceof Player player))
            return null;

        return entity -> entity.viewEngine.viewableOption.addition.accept(player);
    }

    @Nullable
    private Consumer<Entity> createViewerRemovalAction() {
        if (!(owningEntity instanceof Player player))
            return null;

        return entity -> entity.viewEngine.viewableOption.removal.accept(player);
    }

    public void showTo(Player player) {
        // collect the the entities that should be visible to the player (including the vehicle and passengers)
        final List<Entity> visibleEntities = new ArrayList<>();

        // start from the root entity for visibility correctness
        Entity rootEntity = owningEntity;
        while (rootEntity.getVehicle() != null) {
            rootEntity = rootEntity.getVehicle();
        }
//        System.out.println("root entity: " + rootEntity.getEntityType().name());
//        collectVisibleEntityChain(rootEntity, player, visibleEntities);
        collectVisibleEntityChain(owningEntity, player, visibleEntities);

//        System.out.println("------");
//        for (Entity visibleEntity : visibleEntities) {
//            System.out.println(visibleEntity.getEntityType().name());
//            System.out.println("Is Passenger: " + (visibleEntity.getVehicle() != null));
//            System.out.println();
//        }
//        System.out.println("------");

        // check to make sure their are new entities to show the player
        System.out.println(visibleEntities.size());
        if (visibleEntities.isEmpty())
            return;

        // spawn all the newly visible entities, we'll handle passengers after this
        for (Entity visibleEntity : visibleEntities) {
//            final Entity vehicle = visibleEntity.getVehicle();
//            if (vehicle != null) {
//                visiblePassengerIds.computeIfAbsent(vehicle.getEntityId(), _ -> new ArrayList<>()).add(visibleEntity.getEntityId());
//            }

            final List<SendablePacket> packets = visibleEntity.getNewViewerPackets(player);
            player.sendPackets(packets);
        }

        // track all passenger relationships, this fixes desync issues when dealing with complex passenger set-ups
        final Map<Integer, List<Integer>> visiblePassengerIds = new HashMap<>();
        collectVisiblePassengers(rootEntity, player, visiblePassengerIds);

        // send the correct passenger packets for each vehicle
        for (Map.Entry<Integer, List<Integer>> entry : visiblePassengerIds.entrySet()) {
            final int vehicleId = entry.getKey();
            final List<Integer> passengerIds = entry.getValue();
            player.sendPacket(new SetPassengersPacket(vehicleId, passengerIds));
        }
    }

    public void hideFrom(Player player) {
        // TODO: we shouldnt be able to hide a vehicle that we're riding (or any vehicle beneath the vehicle we're riding)

        // lock the two entity views in a consistent ordering to prevent deadlocks
        // the lowest entity ID will be the first lock and the higher entity ID will be the second lock
        final Entity firstLock = player.getEntityId() < owningEntity.getEntityId() ? player : owningEntity;
        final Entity secondLock = firstLock == owningEntity ? player : owningEntity;

        synchronized (firstLock.viewEngine.mutex) {
            synchronized (secondLock.viewEngine.mutex) {
                // register the player for the entity as someone who will not be able to see it
                owningEntity.viewEngine.viewableOption.unregister(player);

                // register the entity from the player as something they can't see
                player.viewEngine.viewerOption.unregister(owningEntity);
            }
        }

        // remove the entity for the player
        player.sendPackets(owningEntity.getOldViewerPackets(player));

        // remove any passengers of this entity as well
        for (Entity passenger : owningEntity.getPassengers()) {
            System.out.println("removing passenger: " + passenger.getEntityType().name());
            passenger.viewEngine.hideFrom(player);
        }
    }

//    private static void showEntityToPlayer(Entity entity, Player player) {
//        // Collects the chain of entities, including the vehicle and all passengers, that should be visible to the player.
//        List<Entity> visibleChain = new ArrayList<>();
//        collectViewableEntityChain(entity, player, visibleChain);
//
//        if (visibleChain.isEmpty()) return;
//
//        // Send spawn packets
//        for (Entity newlyVisibleEntity : visibleChain) {
//            // skip over passengers
//            //if (newlyVisibleEntity.getVehicle() != null)
//            //    continue;
//
//            System.out.println("Send spawn packet for: " + newlyVisibleEntity.getEntityType());
//            newlyVisibleEntity.updateNewViewer(player);
//        }
//
//        // Send passenger packets (in reverse order)
//        for (int i = visibleChain.size() - 1; i >= 0; i--) {
//            Entity newlyVisiblePassenger = visibleChain.get(i);
//            if (newlyVisiblePassenger.getVehicle() != null) {
//                System.out.println("Send passenger packet for: " + newlyVisiblePassenger.getEntityType());
//                player.sendPacket(newlyVisiblePassenger.getVehicle().getPassengersPacket());
//            }
//
////            if (e.hasPassenger() && e.getPassengers().stream().anyMatch(visibleChain::contains)) {
////                System.out.println("Send passenger packet");
////                player.sendPacket(e.getPassengersPacket());
////            }
//

    /// /            // check if this entity is a passenger
    /// /            if (vehicle != null) {
    /// /                vehicle.sendPacketToViewersAndSelf(vehicle.getPassengersPacket());
    /// /            }
    /// /
    /// /            // check if this entity has any passengers that need to be added
    /// /            if (!passengers.isEmpty()) {
    /// /                sendPacketToViewersAndSelf(getPassengersPacket());
    /// /            }
//        }
//    }

    // vehicle should be the root entity, so it will not be on any vehicle
    private static void collectVisiblePassengers(Entity vehicle,
                                                 Player player,
                                                 Map<Integer, List<Integer>> visiblePassengerIds) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger.hasViewer(player)) {
                visiblePassengerIds.computeIfAbsent(vehicle.getEntityId(), _ -> new ArrayList<>()).add(passenger.getEntityId());
                collectVisiblePassengers(passenger, player, visiblePassengerIds);
            }
        }
    }

    /**
     * A recursive function that collects
     *
     * @param player
     * @param visibilityChain
     */
    private static void collectVisibleEntityChain(Entity entity,
                                                  Player player,
                                                  List<Entity> visibilityChain) {
        // lock the two entity views in a consistent ordering to prevent deadlocks
        // the lowest entity ID will be the first lock and the higher entity ID will be the second lock
        final Entity firstLock = player.getEntityId() < entity.getEntityId() ? player : entity;
        final Entity secondLock = firstLock == entity ? player : entity;

        boolean shouldBeViewing = false;
        synchronized (firstLock.viewEngine.mutex) {
            synchronized (secondLock.viewEngine.mutex) {
                if (shouldBeViewing(entity, player)) {
                    System.out.println("should be viewing: " + entity.getEntityType().name());
                    // register the player for the entity as someone who can see it
                    entity.viewEngine.viewableOption.register(player);

                    // register the entity as something the player can see
                    player.viewEngine.viewerOption.register(entity);

                    shouldBeViewing = true;
                }
            }
        }

        // this entity should be visible to the player
        if (shouldBeViewing) {
            visibilityChain.add(entity);

            // check if this entity's passenger(s) (and their passenger(s)) are visible to the player
            // this fixes an issue where passengers are not properly synced when their visibility status is updated
            for (Entity passenger : entity.getPassengers()) {
                collectVisibleEntityChain(passenger, player, visibilityChain);
            }
        }
    }

    private static boolean shouldBeViewing(Entity entity, Player player) {
        // check if the player can already view the entity
        System.out.println("ben 1");
        System.out.println("entity " + entity.getEntityType().name() + " has player viewer: " + entity.hasViewer(player));
        if (entity.hasViewer(player))
            return false;

        System.out.println("ben 2");
        // check if the player is riding the entity
        // if so, then they can already view the entity
        if (player.getVehicle() == entity)
            return false;

        System.out.println("ben 3");
        // make sure that the entity's vehicle chain is visible (if they have one)
        // if the entity's vehicle chain is not visible, then the entity (and any of its passenger(s)) will not be visible either
        Entity vehicle = entity.getVehicle();
        while (vehicle != null) {
            if (!vehicle.viewEngine.viewableOption.predicate(player) || !player.viewEngine.viewerOption.predicate(vehicle))
                return false;

            vehicle = vehicle.getVehicle();
        }
//        if (vehicle != null
//                && (!vehicle.viewEngine.viewableOption.predicate(player) || !player.viewEngine.viewerOption.predicate(vehicle)))
//            return false;

        System.out.println("ben 4");
        // check if the entity registers the player as someone who can see it
        // if not, then this won't be a visible entity
        if (!entity.viewEngine.viewableOption.predicate(player))
            return false;

        System.out.println("ben 5");
        // check if the player registers the entity as something it can see
        // if not, then this won't be a visible entity
        if (!player.viewEngine.viewerOption.predicate(entity))
            return false;

        System.out.println("ben 6");

        return true;
    }

//    private static void collectEntityChain(Entity entity, Player player, List<Entity> chain) {
//        var lock1 = player.getEntityId() < entity.getEntityId() ? player : entity;
//        var lock2 = lock1 == entity ? player : entity;
//
//        boolean shouldAdd = false;
//        synchronized (lock1.viewEngine.mutex) {
//            synchronized (lock2.viewEngine.mutex) {
//                if (!entity.hasViewer(player) &&
//                        player.getVehicle() != entity &&
//                        entity.viewEngine.viewableOption.predicate(player) &&
//                        player.viewEngine.viewerOption.predicate(entity)) {
//
//                    System.out.println("should add: " + entity.getEntityType());
//                    entity.viewEngine.viewableOption.register(player);
//                    player.viewEngine.viewerOption.register(entity);
//                    shouldAdd = true;
//                }
//            }
//        }
//
//        if (shouldAdd) {
//            chain.add(entity);
//        }
//
//        // this fixes the issue where passengers that have their viewing rule updated do not properly sync
//        for (Entity passenger : entity.getPassengers()) {
//            collectEntityChain(passenger, player, chain);
//        }
//

//    / /        if (shouldAdd) {
//    / /            chain.add(entity);
//    / /            for (Entity passenger : entity.getPassengers()) {
//    / /                collectEntityChain(passenger, player, chain);
//    / /            }
//    / /        }
//    }
//    private static void hideEntityFromPlayer(Entity entity, Player player) {
//        var lock1 = player.getEntityId() < entity.getEntityId() ? player : entity;
//        var lock2 = lock1 == entity ? player : entity;
//        synchronized (lock1.viewEngine.mutex) {
//            synchronized (lock2.viewEngine.mutex) {
//                entity.viewEngine.viewableOption.unregister(player);
//                player.viewEngine.viewerOption.unregister(entity);
//            }
//        }
//
//        entity.updateOldViewer(player);
//        final Set<Entity> passengers = entity.getPassengers();
//        if (!passengers.isEmpty()) {
//            for (Entity passenger : passengers) {
//                if (passenger != player) hideEntityFromPlayer(passenger, player);
//            }
//        }
//    }

    /**
     * Updates the current location (instance and point) of this entity.
     * This is useful for viewing purposes because we want to keep track of nearby players and entities.
     *
     * @param instance the instance that this entity is in, or null if none
     * @param point    the point of this entity
     */
    public void updateTracker(@Nullable Instance instance, Point point) {
        this.trackedLocation = instance != null ? new TrackedLocation(instance, point) : null;
    }

    record TrackedLocation(Instance instance, Point point) {
    }

    public boolean manualAdd(Player player) {
        if (player == this.owningEntity)
            return false;

        boolean added;
        synchronized (mutex) {
            added = manualViewers.add(player);
//            if (manualViewers.add(player)) {
//                viewableOption.bitSet.add(player.getEntityId());
//                return true;
//            }
//
//            return false;
        }

        if (added) {
            showTo(player);
        }

        return added;
    }

    public boolean manualRemove(Player player) {
        if (player == this.owningEntity) return false;

        boolean removed;
        synchronized (mutex) {
            removed = manualViewers.remove(player);
        }

        if (removed) {
            hideFrom(player);
        }

        return removed;
    }

    public void forManuals(Consumer<Player> consumer) {
        synchronized (mutex) {
            Set<Player> manualViewersCopy = Set.copyOf(this.manualViewers);
            manualViewersCopy.forEach(consumer);
        }
    }

    public boolean hasPredictableViewers() {
        // Verify if this entity's viewers can be predicted from surrounding entities
        synchronized (mutex) {
            return viewableOption.isAuto() && viewableOption.predicate == null && manualViewers.isEmpty();
        }
    }

    public void handleAutoViewAddition(Entity entity) {
        handleAutoView(entity, viewerOption.addition, viewableOption.addition);
    }

    public void handleAutoViewRemoval(Entity entity) {
        handleAutoView(entity, viewerOption.removal, viewableOption.removal);
    }

    private void handleAutoView(Entity entity, Consumer<Entity> viewer, Consumer<Player> viewable) {
        if (this.owningEntity instanceof Player && viewerOption.isAuto() && entity.isAutoViewable()) {
            if (viewer != null) viewer.accept(entity); // Send packet to this player
        }

        if (entity instanceof Player player && player.autoViewEntities() && viewableOption.isAuto()) {
            if (viewable != null) viewable.accept(player); // Send packet to the range-visible player
        }
    }

    public final class Option<T extends Entity> {

        // allows us to atomically update the autoViewable field, which is just an integer
        // this will let us save some memory if we were to have a lot of spawned entities
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<Option> UPDATER = AtomicIntegerFieldUpdater.newUpdater(Option.class, "auto");

        // the type of entities that should be tracked by this option
        private final EntityTracker.Target<T> target;
        // The condition that must be met for this option to be considered auto.
        private final Predicate<T> loopPredicate;
        // The consumers to be called when an entity is added/removed.
        @Nullable
        public final Consumer<T> addition, removal;
        // Contains all the auto-entity ids that are viewable by this option.
        public final IntSet bitSet = new IntOpenHashSet();
        // 1 if auto, 0 if manual
        private volatile int auto = 1;
        // The custom rule used to determine if an entity is viewable.
        // null if auto-viewable
        @Nullable
        private Predicate<T> predicate;

        public Option(EntityTracker.Target<T> target,
                      Predicate<T> loopPredicate,
                      @Nullable Consumer<T> addition,
                      @Nullable Consumer<T> removal) {
            this.target = target;
            this.loopPredicate = loopPredicate;
            this.addition = addition;
            this.removal = removal;
        }

        public boolean isAuto() {
            return auto == 1;
        }

        public boolean predicate(T entity) {
            final Predicate<T> predicate = this.predicate;
            return predicate == null || predicate.test(entity);
        }

        public boolean isRegistered(T entity) {
            return bitSet.contains(entity.getEntityId());
        }

        public void register(T entity) {
            assert entity.getInstance() != null : "Instance-less entity shouldn't be registered as viewer";
            this.bitSet.add(entity.getEntityId());
        }

        public void unregister(T entity) {
            this.bitSet.remove(entity.getEntityId());
        }

        public void updateAuto(boolean autoViewable) {
            final boolean previous = UPDATER.getAndSet(this, autoViewable ? 1 : 0) == 1;

            // make sure that the previous value is not equal to the new value (autoViewable)
            // if it is, then we don't have to worry about any view updates
            if (previous == autoViewable)
                return;

            synchronized (mutex) {
                if (autoViewable) {
                    System.out.println("Add all potential viewers");
                    update(loopPredicate, addition);
                } else {
                    System.out.println("Remove all viewers");
                    update(this::isRegistered, removal);
                }
            }
        }

        public void updateRule(Predicate<T> predicate) {
            synchronized (mutex) {
                this.predicate = predicate;
                updateRule0(predicate);
            }
        }

        public void updateRule() {
            synchronized (mutex) {
                updateRule0(predicate);
            }
        }

        void updateRule0(Predicate<T> predicate) {
            if (predicate == null) {
                update(loopPredicate, entity -> {
                    if (!isRegistered(entity)) addition.accept(entity);
                });
            } else {
                update(loopPredicate, entity -> {
                    final boolean result = predicate.test(entity);
                    if (result != isRegistered(entity)) {
                        if (result) addition.accept(entity);
                        else removal.accept(entity);
                    }
                });
            }
        }

        private void update(Predicate<T> visibilityPredicate,
                            Consumer<T> action) {
            for (T entity : references()) {
                System.out.println("Test 1");
                // skip over self or invisible entities
                if (entity == EntityView.this.owningEntity || !visibilityPredicate.test(entity))
                    continue;

                System.out.println("Test 2");
                // skip over manual viewers
                if (entity instanceof Player player && manualViewers.contains(player))
                    continue;

                System.out.println("Test 3");
                System.out.println(entity);
                // skip over passengers
                if (entity.getVehicle() != null)
                    continue;

                System.out.println("Test 4");
                action.accept(entity);
                System.out.println();
            }
        }

        private int lastSize;

        private Collection<T> references() {
            final TrackedLocation trackedLocation = EntityView.this.trackedLocation;
            if (trackedLocation == null) return List.of();
            final Instance instance = trackedLocation.instance();
            final Point point = trackedLocation.point();

            Int2ObjectOpenHashMap<T> entityMap = new Int2ObjectOpenHashMap<>(lastSize);
            instance.getEntityTracker().nearbyEntitiesByChunkRange(point, ServerFlag.ENTITY_VIEW_DISTANCE, target,
                    (entity) -> entityMap.putIfAbsent(entity.getEntityId(), entity));
            this.lastSize = entityMap.size();
            return entityMap.values();
        }
    }

    /**
     * An unmodifiable set that holds the current viewers of the owning entity.
     */
    final class SetImpl extends AbstractSet<Player> {

        @Override
        public Iterator<Player> iterator() {
            List<Player> players;
            synchronized (mutex) {
                var bitSet = viewableOption.bitSet;
                if (bitSet.isEmpty()) return Collections.emptyIterator();
                Instance instance = owningEntity.getInstance();
                if (instance == null) return Collections.emptyIterator();
                players = new ArrayList<>(bitSet.size());
                for (IntIterator it = bitSet.intIterator(); it.hasNext(); ) {
                    final int id = it.nextInt();
                    final Player player = (Player) instance.getEntityById(id);
                    if (player != null) players.add(player);
                }
            }

            return players.iterator();
        }

        @Override
        public int size() {
            synchronized (mutex) {
                Instance instance = owningEntity.getInstance();
                if (instance == null) return 0;
                int count = 0;
                for (IntIterator it = viewableOption.bitSet.intIterator(); it.hasNext(); ) {
                    if (instance.getEntityById(it.nextInt()) != null) count++;
                }

                return count;
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (mutex) {
                return viewableOption.bitSet.isEmpty();
            }
        }

        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Player player))
                return false;

            synchronized (mutex) {
                return viewableOption.isRegistered(player);
            }
        }
    }
}