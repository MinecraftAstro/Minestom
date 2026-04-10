package net.minestom.server.entity.view;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
final class ViewEngineUtils {

    private ViewEngineUtils() {
    }

    public static void hideEntityFromPlayer(Entity entity, Player player) {
        hideEntityFromPlayer(entity, player, false);
    }

    public static void hideEntityFromPlayer(Entity entity,
                                            Player player,
                                            boolean previousManualViewer) {
        // make sure that the player is not the passenger of the entity that might be hidden
        // we'll need to check the entire vehicle chain from the player's vehicle to the root (bottom-most) vehicle
        Entity vehicle = player.getVehicle();
        while (vehicle != null) {
            // if this is true, we can't hide this entity since the player relies on seeing it...
            if (vehicle == entity) {
                return;
            }

            vehicle = vehicle.getVehicle();
        }

        hideEntityFromPlayerInternal(entity, player, previousManualViewer);
    }

    private static void hideEntityFromPlayerInternal(Entity entity,
                                                     Player player,
                                                     boolean previousManualViewer) {
        // we can't hide ourselves for ourselves...
        if (player == entity) {
            return;
        }

        // the player is not a viewer of this entity
        if (!entity.hasViewer(player)) {
            return;
        }

        // the player is a manual viewer, this entity can not be hidden for the player
        // this means that even if the entity is out of the view distance for the player, they will still appear for the player in F3 entity count (no destruction packets are sent)
        if (entity.getViewEngine().getEntityView().getManualViewers().contains(player)) {
            return;
        }

        // lock the two entity views in a consistent order to prevent deadlocks
        // the lowest entity ID will be the first lock and the higher entity ID will be the second lock
        final Entity firstLock = player.getEntityId() < entity.getEntityId() ? player : entity;
        final Entity secondLock = firstLock == entity ? player : entity;

        synchronized (firstLock.getViewEngine()) {
            synchronized (secondLock.getViewEngine()) {
                // if the player was a previous manual viewer, we don't want to hide this entity right away
                // instead, we'll check the viewable and viewer rules and see if the player can continue to view the entity
                // if not, then we'll hide them accordingly
                if (previousManualViewer) {
                    if ((entity.isAutoViewable() || entity.getViewableRule().test(player))
                            && (player.isAutoViewEntities() || player.getViewerRule().test(entity))) {
                        // the player can still see this entity, even after being removed as a manual viewer
                        return;
                    }
                }

                // remove the player as a viewer for the entity
                entity.getViewEngine().getEntityView().getViewers().remove(player.getEntityId());

                // remove the entity as a visible entity for the player
                player.getViewEngine().getPlayerView().getVisibleEntities().remove(entity.getEntityId());
            }
        }

        // remove the entity for the player
        player.sendPackets(entity.getRemovedViewerPackets());
        entity.updateRemovedViewer(player);

        // remove any passengers of this entity as well
        for (Entity passenger : entity.getPassengers()) {
            hideEntityFromPlayerInternal(passenger, player, false);
        }
    }

    public static void showEntityToPlayer(Entity entity, Player player) {
        // we can't show ourselves to ourselves...
        if (player == entity)
            return;

        // the player is already a viewer of this entity
        if (entity.hasViewer(player))
            return;

        // collect the newly visible entities for the player
        // this includes the entity's passengers, if they are also visible to the player
        final List<Entity> newlyVisibleEntityChain = new ArrayList<>();
        ViewEngineUtils.collectNewlyVisibleEntityChain(entity, player, newlyVisibleEntityChain);

        // check to make sure that there are new entities to show the player
        // if not, then we don't need to send any packets to the player
        if (newlyVisibleEntityChain.isEmpty())
            return;

        // spawn all the newly visible entities first
        // we'll handle passengers after this
        for (Entity newlyVisibleEntity : newlyVisibleEntityChain) {
            final List<SendablePacket> entitySpawnPackets = newlyVisibleEntity.getNewViewerPackets(player);
            player.sendPackets(entitySpawnPackets);
            entity.updateNewViewer(player);
        }

        // get the correct passenger relationships (with respect to visibility rules)
        // this fixes desync issues when dealing with complex passenger set-ups
        final Map<Integer, List<Integer>> allVisiblePassengerIds = new HashMap<>();
        ViewEngineUtils.collectAllVisiblePassengers(
                ViewEngineUtils.getRootEntity(entity),
                player,
                allVisiblePassengerIds
        );

        // send the correct passenger packets for each vehicle
        for (Map.Entry<Integer, List<Integer>> entry : allVisiblePassengerIds.entrySet()) {
            final int vehicleId = entry.getKey();
            final List<Integer> visiblePassengerIds = entry.getValue();
            player.sendPacket(new SetPassengersPacket(vehicleId, visiblePassengerIds));
        }
    }

    /**
     * Gets the root {@link Entity} of the provided {@link Entity}, which is the bottom-most entity.
     * (an entity that does not have a vehicle).
     *
     * @param entity the entity to get the root entity of
     * @return the root entity
     */
    private static Entity getRootEntity(Entity entity) {
        Entity rootEntity = entity;
        while (rootEntity.getVehicle() != null) {
            rootEntity = rootEntity.getVehicle();
        }

        return rootEntity;
    }

    /**
     * A recursive function that collects all the newly visible and currently visible passengers.
     * This is necessary to be able to send the player the correct passenger packets for the correct vehicles.
     * This is going to be called after {@link #collectNewlyVisibleEntityChain(Entity, Player, List)} so that the
     * player updates their viewer status for the newly visible entities.
     *
     * @param vehicle             the vehicle to get the visible passengers from (typically starting at the root vehicle)
     * @param player              the player that wants to know if a passenger is visible for them
     * @param visiblePassengerIds a collection of vehicle IDs and a list of their passenger IDs
     */
    private static void collectAllVisiblePassengers(Entity vehicle,
                                                    Player player,
                                                    Map<Integer, List<Integer>> visiblePassengerIds) {
        for (Entity passenger : vehicle.getPassengers()) {
            // make sure that the player can view this passenger
            if (passenger.hasViewer(player)) {
                // add the passenger to the collection under its respective vehicle and call this function again for the visible passenger
                visiblePassengerIds.computeIfAbsent(vehicle.getEntityId(), _ -> new ArrayList<>()).add(passenger.getEntityId());
                collectAllVisiblePassengers(passenger, player, visiblePassengerIds);
            }
        }
    }

    // we'll need to send the spawn packets for these
    private static void collectNewlyVisibleEntityChain(Entity entity,
                                                       Player player,
                                                       List<Entity> newlyVisibleEntityChain) {
        // lock the two entity views in a consistent order to prevent deadlocks
        // the lowest entity ID will be the first lock and the higher entity ID will be the second lock
        final Entity firstLock = player.getEntityId() < entity.getEntityId() ? player : entity;
        final Entity secondLock = firstLock == entity ? player : entity;

        boolean isNewlyVisible = false;
        synchronized (firstLock.getViewEngine()) {
            synchronized (secondLock.getViewEngine()) {
                if (isNewlyVisible(entity, player)) {
                    // add the player as a viewer for the entity
                    entity.getViewEngine().getEntityView().getViewers().add(player.getEntityId());

                    // add the entity as a visible entity for the player
                    player.getViewEngine().getPlayerView().getVisibleEntities().add(entity.getEntityId());

                    isNewlyVisible = true;
                }
            }
        }

        if (isNewlyVisible) {
            newlyVisibleEntityChain.add(entity);

            // check if this newly visible entity's passengers (and their passengers) are visible to the player
            // this fixes an issue where passengers are not properly synced when their visibility preferences are updated
            for (Entity passenger : entity.getPassengers()) {
                collectNewlyVisibleEntityChain(passenger, player, newlyVisibleEntityChain);
            }
        }
    }

    /**
     * Determines if an {@link Entity} is newly visible to a specific {@link Player}. This happens
     * when an entity updates its viewability preferences.
     * <br>
     * This function will return false for entities that a player can already see. This
     * function also respects vehicle rules (if it is a passenger), which means that if the
     * entity is a passenger, its chain of vehicles must be visible to the player as well.
     *
     * @param entity the {@link Entity} to check
     * @param player the {@link Player} that wants to know if this entity is newly visible to them
     * @return true if the entity is newly visible, false if not
     */
    private static boolean isNewlyVisible(Entity entity,
                                          Player player) {
        // check if the player can already view the entity
        // if so, then the entity is not newly visible
        if (entity.hasViewer(player))
            return false;

        // check if the player is riding the entity
        // if so, then the player can already view the entity, so it is not newly visible
        if (entity == player.getVehicle())
            return false;

        // make sure that the entity's vehicle chain is visible (if they have one)
        // if the entity's vehicle chain is not visible, then the entity (and any of its passengers) will not be visible either
        Entity vehicle = entity.getVehicle();
        while (vehicle != null) {
            // check if the player can view the vehicle
            if (!vehicle.hasViewer(player))
                return false;

            vehicle = vehicle.getVehicle();
        }

        return entity.getViewEngine().getEntityView().getManualViewers().contains(player) ||
                ((entity.isAutoViewable() || entity.getViewableRule().test(player)) && (player.isAutoViewEntities() || player.getViewerRule().test(entity)));
    }
}