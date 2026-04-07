package net.minestom.server.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
public class PassengerIntegrationTest {

    @Test
    public void addPassengerTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.ZOMBIE);
        final Entity passenger = new Entity(EntityType.SPIDER);

        entity.setInstance(instance, new Pos(0, 42, 0)).join();
        passenger.setInstance(instance, new Pos(0, 42, 0)).join();

        assertEquals(0, entity.getPassengers().size());
        assertNull(passenger.getVehicle());

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
        assertEquals(entity, passenger.getVehicle());
    }

    @Test
    public void addPassengerNonBlockingTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.ZOMBIE);
        final Entity passenger = new Entity(EntityType.SPIDER);

        entity.setInstance(instance, new Pos(0, 42, 0));
        passenger.setInstance(instance, new Pos(0, 42, 0));

        assertEquals(0, entity.getPassengers().size());
        assertNull(passenger.getVehicle());

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
        assertEquals(entity, passenger.getVehicle());
    }

    @Test
    public void addUnspawnedPassengerTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.ZOMBIE);
        final Entity passenger = new Entity(EntityType.SPIDER);

        entity.setInstance(instance, new Pos(0, 42, 0)).join();

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
    }

    @Test
    public void addPassengerFromAnotherInstanceTest(Env env) {
        final Instance instance1 = env.createFlatInstance();
        final Instance instance2 = env.createFlatInstance();

        final Entity entity = new Entity(EntityTypes.ZOMBIE);
        final Entity passenger = new Entity(EntityTypes.SPIDER);

        entity.setInstance(instance1, new Pos(0, 42, 0)).join();
        passenger.setInstance(instance2, new Pos(0, 42, 0)).join();

        assertEquals(0, entity.getPassengers().size());
        assertNull(passenger.getVehicle());

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
        assertEquals(entity, passenger.getVehicle());
        assertEquals(0, instance2.getEntities().size());
    }

    @Test
    public void swapPassengerTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Entity entity1 = new Entity(EntityTypes.ZOMBIE);
        final Entity entity2 = new Entity(EntityTypes.ZOMBIFIED_PIGLIN);
        final Entity passenger = new Entity(EntityTypes.SPIDER);

        entity1.setInstance(instance, new Pos(0, 42, 0)).join();
        entity2.setInstance(instance, new Pos(0, 42, 0)).join();
        passenger.setInstance(instance, new Pos(0, 42, 0)).join();

        entity2.addPassenger(passenger);

        assertEquals(0, entity1.getPassengers().size());
        assertEquals(1, entity2.getPassengers().size());

        entity1.addPassenger(passenger);

        assertEquals(1, entity1.getPassengers().size());
        assertEquals(0, entity2.getPassengers().size());
    }

    @Test
    public void removePassengerTest(Env env) {

    }

    @Test
    public void teleportPassengerTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Entity entity = new Entity(EntityType.ZOMBIE);
        final Entity passenger = new Entity(EntityType.SPIDER);

        entity.setInstance(instance, new Pos(0, 42, 0)).join();
        passenger.setInstance(instance, new Pos(0, 42, 5000)).join();

        entity.addPassenger(passenger);

        assertEquals(1, entity.getPassengers().size());
        assertEquals(entity, passenger.getVehicle());

        passenger.teleport(new Pos(10, 42, 10)).join();

        // TODO: what should happen when a passenger is teleported?
    }
}