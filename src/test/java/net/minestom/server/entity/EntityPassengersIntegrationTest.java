//package net.minestom.server.entity;
//
//import net.minestom.server.coordinate.Pos;
//import net.minestom.server.network.packet.server.play.*;
//import net.minestom.testing.Collector;
//import net.minestom.testing.Env;
//import net.minestom.testing.EnvTest;
//import org.junit.jupiter.api.Test;
//
//import java.util.Collection;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@EnvTest
//public class EntityPassengersIntegrationTest {
//
//    @Test
//    public void testViewingExistingPassengersOnPlayerJoin(Env env) {
//        var instance = env.createFlatInstance();
//
//        final Entity entity = new Entity(EntityType.BEE);
//        final Entity passenger = new Entity(EntityType.ZOMBIE);
//
//        entity.setInstance(instance, new Pos(0, 42, 0));
//        passenger.setInstance(instance, new Pos(0, 42, 0));
//
//        entity.addPassenger(passenger);
//
//        assertEquals(1, entity.getPassengers().size());
//
//        var connection = env.createConnection();
//
//        var player = connection.connect(instance, new Pos(0, 42, 0));
//        assertEquals(instance, player.getInstance());
//        assertEquals(new Pos(0, 42, 0), player.getPosition());
//
//        final Collection<Entity> nearbyEntities = player.getInstance().getNearbyEntities(new Pos(0, 42, 0), 10);
//        for (Entity nearbyEntity : nearbyEntities) {
//            if (nearbyEntity.getEntityType() == EntityTypes.BEE) {
//                assertEquals(1, nearbyEntity.getPassengers().size());
//            }
//        }
//    }
//
//    @Test
//    public void testUpdatingPassengerAutoViewableRule(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//
//        Collector<SetPassengersPacket> setPassengersPacketTracker = connection.trackIncoming(SetPassengersPacket.class);
//
//        // create a player connection so we can track the packets
//        connection.connect(instance, new Pos(0, 42, 0));
//
//        final Entity entity = new Entity(EntityType.BEE);
//        final Entity passenger = new Entity(EntityType.ZOMBIE);
//
//        entity.setInstance(instance, new Pos(0, 42, 0));
//        passenger.setInstance(instance, new Pos(0, 42, 0));
//
//        entity.addPassenger(passenger);
//
//        passenger.setAutoViewable(false);
//        passenger.setAutoViewable(true);
//
//        assertEquals(1, entity.getPassengers().size());
//
//        // 1 for the addPassenger call
//        // 1 for the setAutoViewable(true) call to re-sync passengers once they are viewable again
//        setPassengersPacketTracker.assertCount(2);
//    }
//
//    @Test
//    public void testAddViewerToAutoViewableFalsePassenger(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//
//        Collector<SetPassengersPacket> setPassengersPacketTracker = connection.trackIncoming(SetPassengersPacket.class);
//
//        // create a player connection so we can track the packets
//        var player = connection.connect(instance, new Pos(0, 42, 0));
//
//        final Entity entity = new Entity(EntityType.BEE);
//        final Entity passenger = new Entity(EntityType.ZOMBIE);
//        final Entity secondaryPassenger = new Entity(EntityType.SKELETON);
//        passenger.setAutoViewable(false);
//
//        entity.setInstance(instance, new Pos(0, 42, 0));
//        passenger.setInstance(instance, new Pos(0, 42, 0));
//        secondaryPassenger.setInstance(instance, new Pos(0, 42, 0));
//
//        entity.addPassenger(passenger);
//        passenger.addPassenger(secondaryPassenger);
//
//        entity.addViewer(player);
//
//        assertEquals(1, entity.getPassengers().size());
//
//        // 2 for the addPassenger calls
//        // 1 for the addViewer call to re-sync passengers for the viewer
//        setPassengersPacketTracker.assertCount(3);
//    }
//
//    @Test
//    public void testRemovingPassenger(Env env) {
//        var instance = env.createFlatInstance();
//
//        final Entity entity = new Entity(EntityType.BEE);
//        final Entity passenger = new Entity(EntityType.ZOMBIE);
//
//        entity.setInstance(instance, new Pos(0, 42, 0));
//        passenger.setInstance(instance, new Pos(0, 42, 0));
//
//        entity.addPassenger(passenger);
//
//        assertEquals(1, entity.getPassengers().size());
//
//        entity.removePassenger(passenger);
//
//        assertEquals(0, entity.getPassengers().size());
//    }
//}