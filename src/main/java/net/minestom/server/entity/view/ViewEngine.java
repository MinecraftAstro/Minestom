package net.minestom.server.entity.view;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

public final class ViewEngine {

    private final Entity entity;

    private final EntityView entityView;
    @Nullable
    private PlayerView playerView;

    @Nullable
    private volatile TrackedLocation trackedLocation;

    private final Set<Player> viewers = new ViewerSet();

    // useful to prevent collection resizing
    private int previousNearbyPlayersCount;
    private int previousNearbyEntitiesCount;

    private final Object lock = this;

    public ViewEngine(Entity entity) {
        this.entity = entity;

        this.entityView = new EntityView(entity);
        if (entity instanceof Player player) {
            this.playerView = new PlayerView(player);
        }
    }

    // manually add a viewer
    // this goes away when viewable/viewer rules change
    // TODO: should we have a manual viewer?
    public boolean addViewer(Player player) {
        return false;
    }

    // manually remove a viewer
    // this goes away when viewable/viewer rules change
    // TODO: should we have a manual viewer?
    public boolean removeViewer(Player player) {
        return false;
    }

    public synchronized boolean hide() {
        final Predicate<Player> newViewableRule = _ -> false;

        entityView.setViewableRule(newViewableRule);
        entityView.setAutoViewable(false);

        handleViewableRuleUpdate(newViewableRule);
        return true;
    }

    public synchronized boolean show() {
        if (entityView.isAutoViewable())
            return false;

        final Predicate<Player> newViewableRule = _ -> true;

        entityView.setViewableRule(newViewableRule);
        entityView.setAutoViewable(true);

        handleViewableRuleUpdate(newViewableRule);
        return true;
    }

    // player only
    public boolean hideEntities() {
        if (playerView == null)
            return false;

        synchronized (lock) {
            final Predicate<Entity> newViewerRule = _ -> false;

            playerView.setViewerRule(newViewerRule);
            playerView.setAutoViewEntities(false);

            handleViewerRuleUpdate(newViewerRule);
            return true;
        }
    }

    // player only
    public boolean showEntities() {
        if (playerView == null)
            return false;

        synchronized (lock) {
            final Predicate<Entity> newViewerRule = _ -> true;

            playerView.setViewerRule(newViewerRule);
            playerView.setAutoViewEntities(true);

            handleViewerRuleUpdate(newViewerRule);
            return true;
        }
    }

    public synchronized void setViewableRule(Predicate<Player> viewableRule) {
        entityView.setViewableRule(viewableRule);
        entityView.setAutoViewable(false);

        handleViewableRuleUpdate(viewableRule);
    }

    // player only
    public boolean setViewerRule(Predicate<Entity> viewerRule) {
        if (playerView == null)
            return false;

        synchronized (lock) {
            playerView.setViewerRule(viewerRule);
            playerView.setAutoViewEntities(false);

            handleViewerRuleUpdate(viewerRule);
            return true;
        }
    }

    // internal method
    private void handleViewableRuleUpdate(Predicate<Player> viewableRule) {
        for (Player nearbyPlayer : getNearbyPlayers()) {
            // the entity is now auto-viewable, attempt to show to all nearby players (we still have to worry about player viewer rules)
            if (entityView.isAutoViewable()) {
                ViewEngineUtils.showEntityToPlayer(this.entity, nearbyPlayer);
                continue;
            }

            // check if the player meets the new viewable rule
            if (viewableRule.test(nearbyPlayer)) {
                ViewEngineUtils.showEntityToPlayer(this.entity, nearbyPlayer);
            } else {
                ViewEngineUtils.hideEntityFromPlayer(this.entity, nearbyPlayer);
            }
        }
    }

    // internal method
    private void handleViewerRuleUpdate(Predicate<Entity> viewerRule) {
        final Player player = playerView.getPlayer();
        for (Entity nearbyEntity : getNearbyEntities()) {
            // the player can now auto-view entities, attempt to show the entity (we still have to worry about entity viewable rules)
            if (playerView.isAutoViewEntities()) {
                ViewEngineUtils.showEntityToPlayer(nearbyEntity, player);
            }

            // check if the entity meets the new viewer rule
            if (viewerRule.test(nearbyEntity)) {
                ViewEngineUtils.showEntityToPlayer(nearbyEntity, player);
            } else {
                ViewEngineUtils.hideEntityFromPlayer(nearbyEntity, player);
            }
        }
    }

    public EntityView getEntityView() {
        return entityView;
    }

    @Nullable
    public PlayerView getPlayerView() {
        return playerView;
    }

    public synchronized void handleTrackerAddition(Entity entity) {
        System.out.println("Handle entity addition: " + this.entity.getEntityType());

        if (playerView != null) {
            ViewEngineUtils.showEntityToPlayer(entity, playerView.getPlayer());
        }

        if (entity instanceof Player player) {
            ViewEngineUtils.showEntityToPlayer(this.entity, player);
        }
    }

    public synchronized void handleTrackerRemoval(Entity entity) {
        if (playerView != null) {
            ViewEngineUtils.hideEntityFromPlayer(entity, playerView.getPlayer());
        }

        if (entity instanceof Player player) {
            ViewEngineUtils.hideEntityFromPlayer(this.entity, player);
        }
    }

    public void handleTrackerUpdate(@Nullable Instance instance, Point point) {
        this.trackedLocation = instance != null ? new TrackedLocation(instance, point) : null;
    }

    record TrackedLocation(Instance instance, Point point) {
    }

    private Collection<Player> getNearbyPlayers() {
        final TrackedLocation lastTrackedLocation = trackedLocation;
        if (lastTrackedLocation == null)
            return Collections.emptyList();

        final Instance instance = lastTrackedLocation.instance();
        final Point point = lastTrackedLocation.point();

        final Int2ObjectMap<Player> nearbyPlayers = new Int2ObjectOpenHashMap<>(previousNearbyPlayersCount);
        instance.getEntityTracker().nearbyEntitiesByChunkRange(
                point,
                ServerFlag.ENTITY_VIEW_DISTANCE,
                EntityTracker.Target.PLAYERS,
                player -> nearbyPlayers.putIfAbsent(player.getEntityId(), player)
        );

        previousNearbyPlayersCount = nearbyPlayers.size();
        return nearbyPlayers.values();
    }

    private Collection<Entity> getNearbyEntities() {
        final TrackedLocation lastTrackedLocation = trackedLocation;
        if (lastTrackedLocation == null)
            return Collections.emptyList();

        final Instance instance = lastTrackedLocation.instance();
        final Point point = lastTrackedLocation.point();

        final Int2ObjectMap<Entity> nearbyEntities = new Int2ObjectOpenHashMap<>(previousNearbyEntitiesCount);
        instance.getEntityTracker().nearbyEntitiesByChunkRange(
                point,
                ServerFlag.ENTITY_VIEW_DISTANCE,
                EntityTracker.Target.ENTITIES,
                entity -> nearbyEntities.putIfAbsent(entity.getEntityId(), entity)
        );

        previousNearbyEntitiesCount = nearbyEntities.size();
        return nearbyEntities.values();
    }

    @Unmodifiable
    public Set<Player> getViewers() {
        return viewers;
    }

    /**
     * An unmodifiable set that holds the current viewers of the {@code entity}.
     */
    final class ViewerSet extends AbstractSet<Player> {

        @Override
        public Iterator<Player> iterator() {
            final List<Player> players;
            synchronized (lock) {
                final IntSet viewers = entityView.getViewers();
                if (viewers.isEmpty())
                    return Collections.emptyIterator();

                final Instance instance = entity.getInstance();
                if (instance == null)
                    return Collections.emptyIterator();

                players = new ArrayList<>(viewers.size());
                for (IntIterator iterator = viewers.iterator(); iterator.hasNext(); ) {
                    final int playerId = iterator.nextInt();
                    final Player player = (Player) instance.getEntityById(playerId);
                    if (player != null)
                        players.add(player);
                }
            }

            return players.iterator();
        }

        @Override
        public int size() {
            synchronized (lock) {
                final Instance instance = entityView.getEntity().getInstance();
                if (instance == null)
                    return 0;

                int count = 0;
                for (IntIterator iterator = entityView.getViewers().iterator(); iterator.hasNext(); ) {
                    final int viewerId = iterator.nextInt();
                    if (instance.getEntityById(viewerId) != null)
                        count++;
                }

                return count;
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (lock) {
                return entityView.getViewers().isEmpty();
            }
        }

        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Player player))
                return false;

            synchronized (lock) {
                return entityView.getViewers().contains(player.getEntityId());
            }
        }
    }
}