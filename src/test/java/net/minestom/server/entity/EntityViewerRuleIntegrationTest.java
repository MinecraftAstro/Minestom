//package net.minestom.server.entity;
//
//import net.minestom.server.coordinate.Pos;
//import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
//import net.minestom.testing.Env;
//import net.minestom.testing.EnvTest;
//import org.junit.jupiter.api.Test;
//
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@EnvTest
//public class EntityViewerRuleIntegrationTest {
//
//    @Test
//    public void passengerRespectsViewableRuleOnJoin(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var spawnTracker = connection.trackIncoming(SpawnEntityPacket.class);
//
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//        var passenger = new Entity(EntityType.PIG);
//        passenger.updateViewableRule(p -> false);
//        vehicle.addPassenger(passenger);
//
//        var testPlayer = connection.connect(instance, new Pos(0, 40, 0));
//
//        var spawns = spawnTracker.collect().stream()
//                .filter(p -> p.entityId() != testPlayer.getEntityId())
//                .toList();
//        assertEquals(1, spawns.size());
//        assertEquals(vehicle.getEntityId(), spawns.getFirst().entityId());
//    }
//
//    @Test
//    public void passengerRespectsViewableRuleChange(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var spawnTracker = connection.trackIncoming(SpawnEntityPacket.class);
//
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//        var passenger = new Entity(EntityType.PIG);
//        vehicle.addPassenger(passenger);
//
//        var testPlayer = connection.connect(instance, new Pos(0, 40, 0));
//
//        var spawns = spawnTracker.collect().stream()
//                .filter(p -> p.entityId() != testPlayer.getEntityId())
//                .toList();
//        assertEquals(2, spawns.size());
//
//        passenger.updateViewableRule(p -> false);
//
//        assertTrue(vehicle.getViewers().contains(testPlayer));
//        assertFalse(passenger.getViewers().contains(testPlayer));
//    }
//
//
//    @Test
//    public void vehicleViewableRuleChange(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var spawnTracker = connection.trackIncoming(SpawnEntityPacket.class);
//
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//        var passenger = new Entity(EntityType.PIG);
//        vehicle.addPassenger(passenger);
//
//        var testPlayer = connection.connect(instance, new Pos(0, 40, 0));
//
//        var spawns = spawnTracker.collect().stream()
//                .filter(p -> p.entityId() != testPlayer.getEntityId())
//                .toList();
//        assertEquals(2, spawns.size());
//
//        vehicle.updateViewableRule(p -> false);
//
//        assertFalse(vehicle.getViewers().contains(testPlayer));
//        assertFalse(passenger.getViewers().contains(testPlayer));
//    }
//
//    @Test
//    public void manualViewerOnlySeesVehicle(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var spawnTracker1 = connection.trackIncoming(SpawnEntityPacket.class);
//        var spawnTracker2 = connection.trackIncoming(SpawnEntityPacket.class);
//
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        var passenger = new Entity(EntityType.PIG);
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//
//        vehicle.setAutoViewable(false);
//        passenger.setAutoViewable(false);
//        vehicle.addPassenger(passenger);
//
//        var testPlayer = connection.connect(instance, new Pos(0, 40, 5000));
//        spawnTracker1.assertCount(0);
//
//        vehicle.addViewer(testPlayer);
//
//        spawnTracker2.assertCount(1);
//        assertTrue(vehicle.hasViewer(testPlayer));
//        assertFalse(passenger.hasViewer(testPlayer));
//    }
//
//    @Test
//    public void manualViewerRespectsPassengerRule(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var spawnTracker1 = connection.trackIncoming(SpawnEntityPacket.class);
//        var spawnTracker2 = connection.trackIncoming(SpawnEntityPacket.class);
//
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        var passenger = new Entity(EntityType.PIG);
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//
//        vehicle.setAutoViewable(false);
//        passenger.updateViewableRule(p -> false);
//        vehicle.addPassenger(passenger);
//
//        var testPlayer = connection.connect(instance, new Pos(0, 40, 5000));
//        spawnTracker1.assertCount(0);
//
//        vehicle.addViewer(testPlayer);
//
//        spawnTracker2.assertCount(1);
//        assertEquals(vehicle.getEntityId(), spawnTracker2.collect().getFirst().entityId());
//        assertTrue(vehicle.hasViewer(testPlayer));
//        assertFalse(passenger.hasViewer(testPlayer));
//    }
//}