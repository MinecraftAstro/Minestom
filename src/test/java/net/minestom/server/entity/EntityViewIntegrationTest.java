//package net.minestom.server.entity;
//
//import net.minestom.server.coordinate.Pos;
//import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
//import net.minestom.testing.Env;
//import net.minestom.testing.EnvTest;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@EnvTest
//public class EntityViewIntegrationTest {
//
//    @Test
//    public void livingVehicle(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var player = connection.connect(instance, new Pos(0, 40, 0));
//
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        var passenger = new Entity(EntityType.ZOMBIE);
//
//        var tracker = connection.trackIncoming(SpawnEntityPacket.class);
//
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//        vehicle.addPassenger(passenger);
//        // Verify packets
//        {
//            var results = tracker.collect();
//            assertEquals(2, results.size());
//            assertEquals(vehicle.getEntityId(), results.get(0).entityId());
//            assertEquals(passenger.getEntityId(), results.get(1).entityId());
//        }
//        // Verify viewers
//        {
//            assertEquals(0, player.getViewers().size());
//            assertEquals(1, vehicle.getViewers().size());
//            assertTrue(vehicle.hasViewer(player));
//            assertEquals(1, passenger.getViewers().size());
//            assertTrue(passenger.hasViewer(player));
//        }
//    }
//
//    @Test
//    public void sendsSpawnPacketsToExistingViewers(Env env) {
//        var instance = env.createFlatInstance();
//        var connection = env.createConnection();
//        var vehicle = new Entity(EntityType.ZOMBIE);
//        var passenger = new Entity(EntityType.ZOMBIE);
//
//        vehicle.setInstance(instance, new Pos(0, 40, 0)).join();
//        vehicle.addPassenger(passenger);
//
//        var tracker = connection.trackIncoming(SpawnEntityPacket.class);
//        var player = connection.connect(instance, new Pos(0, 40, 0));
//
//        var spawns = tracker.collect().stream()
//                .filter(p -> p.entityId() != player.getEntityId()).toList();
//        assertEquals(2, spawns.size());
//
//        assertEquals(1, vehicle.getViewers().size());
//        assertTrue(vehicle.hasViewer(player));
//        assertEquals(1, passenger.getViewers().size());
//        assertTrue(passenger.hasViewer(player));
//    }
//
//    @Test
//    public void vehicleInheritance(Env env) {
//        var instance = env.createFlatInstance();
//        var p1 = env.createPlayer(instance, new Pos(0, 40, 0));
//        var p2 = env.createPlayer(instance, new Pos(0, 40, 0));
//
//        var vehicle1 = new Entity(EntityType.ZOMBIE);
//        vehicle1.setInstance(instance, new Pos(0, 40, 0)).join();
//        vehicle1.addPassenger(p1);
//
//        var vehicle2 = new Entity(EntityType.ZOMBIE);
//        vehicle2.setInstance(instance, new Pos(0, 40, 0)).join();
//        vehicle2.addPassenger(p2);
//
//        assertEquals(2, vehicle1.getViewers().size());
//        assertTrue(vehicle1.getViewers().contains(p2));
//
//        assertEquals(2, vehicle2.getViewers().size());
//        assertTrue(vehicle2.getViewers().contains(p1));
//    }
//
//    @Test
//    public void sizeMatchesIteratorIncludingNullPlayers(Env env) {
//        var instance = env.createFlatInstance();
//        var entity = new Entity(EntityType.ZOMBIE);
//        entity.setInstance(instance, new Pos(0, 40, 0)).join();
//        var set = entity.getViewers();
//
//        env.createPlayer(instance, new Pos(0, 40, 0));
//        assertEquals(1, set.size());
//
//        entity.viewEngine.viewableOption.bitSet.add(-1);
//
//        assertEquals(1, set.size());
//
//        long iteratorCount = 0;
//        for (var _ : set) iteratorCount++;
//        assertEquals(set.size(), iteratorCount);
//    }
//}
