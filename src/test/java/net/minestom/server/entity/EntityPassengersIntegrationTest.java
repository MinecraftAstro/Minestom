package net.minestom.server.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

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
}