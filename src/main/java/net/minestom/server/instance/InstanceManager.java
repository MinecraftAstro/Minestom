package net.minestom.server.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.InstanceRegisterEvent;
import net.minestom.server.event.instance.InstanceUnregisterEvent;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Used to register {@link Instance}.
 */
public final class InstanceManager {

    private final Registries registries;
    private final Set<Instance> instances = new CopyOnWriteArraySet<>();

    public InstanceManager(@NotNull Registries registries) {
        this.registries = registries;
    }

    /**
     * Registers an {@link Instance} internally.
     * <p>
     * Note: not necessary if you created your instance using {@link #createInstanceContainer()} or {@link #createSharedInstance(InstanceContainer)}
     * but only if you instantiated your instance object manually
     *
     * @param instance the {@link Instance} to register
     */
    public void registerInstance(@NotNull Instance instance) {
        Check.stateCondition(instance instanceof SharedInstance,
                "Please use InstanceManager#registerSharedInstance to register a shared instance");
        UNSAFE_registerInstance(instance);
    }

    /**
     * Creates and register an {@link InstanceContainer} with the specified {@link DimensionType}.
     *
     * @param dimensionType the {@link DimensionType} of the instance
     * @param loader        the chunk loader
     * @return the created {@link InstanceContainer}
     */
    public @NotNull InstanceContainer createInstanceContainer(@NotNull RegistryKey<DimensionType> dimensionType, @Nullable IChunkLoader loader) {
        final InstanceContainer instanceContainer = new InstanceContainer(registries.dimensionType(), UUID.randomUUID(), dimensionType, loader, dimensionType.key());
        registerInstance(instanceContainer);
        return instanceContainer;
    }

    public @NotNull InstanceContainer createInstanceContainer(@NotNull RegistryKey<DimensionType> dimensionType) {
        return createInstanceContainer(dimensionType, null);
    }

    public @NotNull InstanceContainer createInstanceContainer(@Nullable IChunkLoader loader) {
        return createInstanceContainer(DimensionType.OVERWORLD, loader);
    }

    /**
     * Creates and register an {@link InstanceContainer}.
     *
     * @return the created {@link InstanceContainer}
     */
    public @NotNull InstanceContainer createInstanceContainer() {
        return createInstanceContainer(DimensionType.OVERWORLD, null);
    }

    /**
     * Registers a {@link SharedInstance}.
     * <p>
     * WARNING: the {@link SharedInstance} needs to have an {@link InstanceContainer} assigned to it.
     *
     * @param sharedInstance the {@link SharedInstance} to register
     * @return the registered {@link SharedInstance}
     * @throws NullPointerException if {@code sharedInstance} doesn't have an {@link InstanceContainer} assigned to it
     */
    public @NotNull SharedInstance registerSharedInstance(@NotNull SharedInstance sharedInstance) {
        final InstanceContainer instanceContainer = sharedInstance.getInstanceContainer();
        Check.notNull(instanceContainer, "SharedInstance needs to have an InstanceContainer to be created!");

        instanceContainer.addSharedInstance(sharedInstance);
        UNSAFE_registerInstance(sharedInstance);
        return sharedInstance;
    }

    /**
     * Creates and register a {@link SharedInstance}.
     *
     * @param instanceContainer the container assigned to the shared instance
     * @return the created {@link SharedInstance}
     * @throws IllegalStateException if {@code instanceContainer} is not registered
     */
    public @NotNull SharedInstance createSharedInstance(@NotNull InstanceContainer instanceContainer) {
        Check.notNull(instanceContainer, "Instance container cannot be null when creating a SharedInstance!");
        Check.stateCondition(!instanceContainer.isRegistered(), "The container needs to be register in the InstanceManager");

        final SharedInstance sharedInstance = new SharedInstance(UUID.randomUUID(), instanceContainer);
        return registerSharedInstance(sharedInstance);
    }

    /**
     * Unregisters the {@link Instance} internally.
     * <p>
     * If {@code instance} is an {@link InstanceContainer} all chunks are unloaded.
     *
     * @param instance the {@link Instance} to unregister
     */
    public void unregisterInstance(@NotNull Instance instance) {
        long onlinePlayers = instance.getPlayers().stream().filter(Player::isOnline).count();
        Check.stateCondition(onlinePlayers > 0, "You cannot unregister an instance with players inside.");
        synchronized (instance) {
            InstanceUnregisterEvent event = new InstanceUnregisterEvent(instance);
            EventDispatcher.call(event);

            // Unload all chunks
            if (instance instanceof InstanceContainer) {
                instance.getChunks().forEach(instance::unloadChunk);
                var dispatcher = MinecraftServer.process().dispatcher();
                instance.getChunks().forEach(dispatcher::deletePartition);
            }
            // Unregister
            instance.setRegistered(false);
            this.instances.remove(instance);
        }
    }

    /**
     * Gets all the registered instances.
     *
     * @return an unmodifiable {@link Set} containing all the registered instances
     */
    public @NotNull Set<@NotNull Instance> getInstances() {
        return Collections.unmodifiableSet(instances);
    }

    /**
     * Gets an instance by the given UUID.
     *
     * @param uuid UUID of the instance
     * @return the instance with the given UUID, null if not found
     */
    public @Nullable Instance getInstance(@NotNull UUID uuid) {
        Optional<Instance> instance = getInstances()
                .stream()
                .filter(someInstance -> someInstance.getUuid().equals(uuid))
                .findFirst();
        return instance.orElse(null);
    }

    /**
     * Registers an {@link Instance} internally.
     * <p>
     * Unsafe because it does not check if {@code instance} is a {@link SharedInstance} to verify its container.
     *
     * @param instance the {@link Instance} to register
     */
    private void UNSAFE_registerInstance(@NotNull Instance instance) {
        instance.setRegistered(true);
        this.instances.add(instance);
        var dispatcher = MinecraftServer.process().dispatcher();
        instance.getChunks().forEach(dispatcher::createPartition);
        InstanceRegisterEvent event = new InstanceRegisterEvent(instance);
        EventDispatcher.call(event);
    }
}
