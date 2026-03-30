package net.minestom.server.entity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.utils.PositionUtilsTest;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
public class EntityPassengersIntegrationTest {

    @Test
    public void testAddingUnspawnedPassenger(Env env) {
        var instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger1 = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance, new Pos(0, 42, 0)).join();

        entity.addPassenger(passenger1);

        assertEquals(1, entity.getPassengers().size());
    }

    @Test
    public void testAddingPassengerFromAnotherInstance(Env env) {
        var instance1 = env.createFlatInstance();
        var instance2 = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance1, new Pos(0, 42, 0)).join();
        passenger.setInstance(instance2, new Pos(0, 42, 0)).join();

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
    }

    @Test
    public void testAddingPassengerRidingAnotherVehicle(Env env) {
        var instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.BEE);
        final Entity otherEntity = new Entity(EntityType.SKELETON);
        final Entity passenger = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance, new Pos(0, 42, 0)).join();
        otherEntity.setInstance(instance, new Pos(0, 42, 0)).join();
        passenger.setInstance(instance, new Pos(0, 42, 0)).join();

        otherEntity.addPassenger(passenger);
        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
        assertEquals(0, otherEntity.getPassengers().size());
    }

    @Test
    public void testAddingPassengerWithoutJoiningFuture(Env env) {
        var instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance, new Pos(0, 42, 0));
        passenger.setInstance(instance, new Pos(0, 42, 0));

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
    }

    @Test
    public void testViewingExistingPassengersOnPlayerJoin(Env env) {
        var instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance, new Pos(0, 42, 0));
        passenger.setInstance(instance, new Pos(0, 42, 0));

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());

        var connection = env.createConnection();

        var player = connection.connect(instance, new Pos(0, 42, 0));
        assertEquals(instance, player.getInstance());
        assertEquals(new Pos(0, 42, 0), player.getPosition());

        final Collection<Entity> nearbyEntities = player.getInstance().getNearbyEntities(new Pos(0, 42, 0), 10);
        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity.getEntityType() == EntityTypes.BEE) {
                assertEquals(1, nearbyEntity.getPassengers().size());
            }
        }
    }

    @Test
    public void testUpdatingPassengerAutoViewableRule(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();

        Collector<SetPassengersPacket> setPassengersPacketTracker = connection.trackIncoming(SetPassengersPacket.class);

        // create a player connection so we can track the packets
        connection.connect(instance, new Pos(0, 42, 0));

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance, new Pos(0, 42, 0));
        passenger.setInstance(instance, new Pos(0, 42, 0));

        entity.addPassenger(passenger);

        passenger.setAutoViewable(false);
        passenger.setAutoViewable(true);

        assertEquals(1, entity.getPassengers().size());

        // 1 for the addPassenger call
        // 1 for the setAutoViewable(true) call to re-sync passengers once they are viewable again
        setPassengersPacketTracker.assertCount(2);
    }

    @Test
    public void testAddViewerToAutoViewableFalsePassenger(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();

        Collector<SetPassengersPacket> setPassengersPacketTracker = connection.trackIncoming(SetPassengersPacket.class);

        // create a player connection so we can track the packets
        var player = connection.connect(instance, new Pos(0, 42, 0));

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger = new Entity(EntityType.ZOMBIE);
        final Entity secondaryPassenger = new Entity(EntityType.SKELETON);
        passenger.setAutoViewable(false);

        entity.setInstance(instance, new Pos(0, 42, 0));
        passenger.setInstance(instance, new Pos(0, 42, 0));
        secondaryPassenger.setInstance(instance, new Pos(0, 42, 0));

        entity.addPassenger(passenger);
        passenger.addPassenger(secondaryPassenger);

        entity.addViewer(player);

        assertEquals(1, entity.getPassengers().size());

        // 2 for the addPassenger calls
        // 1 for the addViewer call to re-sync passengers for the viewer
        setPassengersPacketTracker.assertCount(3);
    }

    @Test
    public void testRemovingPassenger(Env env) {
        var instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.BEE);
        final Entity passenger = new Entity(EntityType.ZOMBIE);

        entity.setInstance(instance, new Pos(0, 42, 0));
        passenger.setInstance(instance, new Pos(0, 42, 0));

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());

        entity.removePassenger(passenger);

        assertEquals(0, entity.getPassengers().size());
    }
}