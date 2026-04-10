package net.minestom.demo;

import net.kyori.adventure.Adventure;
import net.kyori.adventure.internal.properties.AdventureProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.demo.commands.*;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.metadata.animal.SheepMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.StringUtils;
import net.minestom.server.utils.time.TimeUnit;

public class Main {

    private static Entity zombie;
    private static Entity passenger;

    static void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        InstanceContainer container = MinecraftServer.getInstanceManager().createInstanceContainer();
        container.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        container.setChunkSupplier(LightingChunk::new);

        GlobalEventHandler node = MinecraftServer.getGlobalEventHandler();
        node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(container);
            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
        });

        node.addListener(PlayerStartSneakingEvent.class, event -> {
            Player player = event.getPlayer();
            zombie = new Entity(EntityType.ZOMBIE);

            final Entity leashedEntity = new Entity(EntityType.COW);
            leashedEntity.setInstance(player.getInstance(), player.getPosition().add(5, 5, 0)).join();

            zombie.setInstance(player.getInstance(), player.getPosition().add(0, 5, 0)).whenComplete((_, throwable) -> {
                if (throwable != null) return;
                System.out.println("creating passenger");
                passenger = createPassenger(zombie);
                passenger.addViewer(player);

//                MinecraftServer.getSchedulerManager().buildTask(() -> {
//                    System.out.println("Showing passenger to all!");
//                    passenger.show();
//                }).delay(TaskSchedule.seconds(5L)).schedule();
//
//                MinecraftServer.getSchedulerManager().buildTask(() -> {
//                    System.out.println("Removing manual viewer...");
//                    passenger.removeViewer(player);
//                }).delay(TaskSchedule.seconds(10L)).schedule();

                leashedEntity.setLeashHolder(zombie);
            });
        });

        node.addListener(PlayerSwapItemEvent.class, event -> {
            final Player player = event.getPlayer();

            zombie.teleport(player.getPosition()).join();

//            player.kill();
//            player.respawn();
        });

        node.addListener(EntityAttackEvent.class, event -> {
            final Entity source = event.getEntity();
            final Entity entity = event.getTarget();

            entity.takeKnockback(0.4f, Math.sin(source.getPosition().yaw() * 0.017453292), -Math.cos(source.getPosition().yaw() * 0.017453292));

            if (entity instanceof Player) {
                Player target = (Player) entity;
                target.damage(Damage.fromEntity(source, 5));
            }

            if (source instanceof Player) {
                ((Player) source).sendMessage("You attacked something!");
            }
        });

        server.start("0.0.0.0", 25565);
    }

    private static Entity createPassenger(Entity owner) {
        Entity passenger = new Entity(EntityType.SPIDER);
        passenger.hide();
        passenger.setInstance(owner.getInstance(), owner.getPosition().add(5, 0, 0)).whenComplete((_, throwable) -> {
            if (throwable != null) return;
            owner.addPassenger(passenger);
        }).join();
        return passenger;
    }

//    private static Entity zombie;
//    private static Entity passenger;
//    private static Entity secondaryPassenger;
//
//    static void main() {
//        MinecraftServer server = MinecraftServer.init();
//
//        InstanceContainer container = MinecraftServer.getInstanceManager().createInstanceContainer();
//        container.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
//        container.setChunkSupplier(LightingChunk::new);
//
//        GlobalEventHandler node = MinecraftServer.getGlobalEventHandler();
//        node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
//            event.setSpawningInstance(container);
//            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
//        });
////
////        node.addListener(PlayerStartSneakingEvent.class, event -> {
////            Player player = event.getPlayer();
////            zombie = new Entity(EntityType.ZOMBIE);
////            zombie.setInstance(player.getInstance(), player.getPosition().add(0, 5, 0)).whenComplete((_, throwable) -> {
////                if (throwable != null) return;
////                passenger = createPassenger(zombie);
////                secondaryPassenger = createPassenger(passenger);
////
////                passenger.addViewer(player);
////            });
////        });
////
//        node.addListener(PlayerSwapItemEvent.class, event -> {
//            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
//            event.getPlayer().kill();
//            event.getPlayer().respawn();
//        });
//
//        node.addListener(EntityAttackEvent.class, event -> {
//            final Entity source = event.getEntity();
//            final Entity entity = event.getTarget();
//
//            entity.takeKnockback(0.4f, Math.sin(source.getPosition().yaw() * 0.017453292), -Math.cos(source.getPosition().yaw() * 0.017453292));
//
//            if (entity instanceof Player) {
//                Player target = (Player) entity;
//                target.damage(Damage.fromEntity(source, 5));
//            }
//
//            if (source instanceof Player) {
//                ((Player) source).sendMessage("You attacked something!");
//            }
//        });
////
////        node.addListener(EntityDespawnEvent.class, event -> {
////            if (!event.getEntity().equals(zombie)) return;
////            System.out.println("removing passenger");
////            passenger.remove();
////        });
//
//        server.start("0.0.0.0", 25565);
//    }
//
//    private static Entity createPassenger(Entity owner) {
//        Entity passenger = new Entity(EntityType.SPIDER);
//        passenger.setAutoViewable(false);
//        passenger.setInstance(owner.getInstance(), owner.getPosition().add(5, 0, 0)).whenComplete((_, throwable) -> {
//            if (throwable != null) return;
//            owner.addPassenger(passenger);
//        });
//        return passenger;
//    }
//
////    static void main(String[] args) {
////        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());
////
////        CommandManager commandManager = MinecraftServer.getCommandManager();
////        commandManager.register(new AroundCommand());
////        commandManager.register(new ComeCommand());
////        commandManager.register(new TestCommand());
////        commandManager.register(new EntitySelectorCommand());
////        commandManager.register(new HealthCommand());
////        commandManager.register(new LegacyCommand());
////        commandManager.register(new DimensionCommand());
////        commandManager.register(new ShutdownCommand());
////        commandManager.register(new TeleportCommand());
////        commandManager.register(new PlayersCommand());
////        commandManager.register(new FindCommand());
////        commandManager.register(new TitleCommand());
////        commandManager.register(new BookCommand());
////        commandManager.register(new ShootCommand());
////        commandManager.register(new HorseCommand());
////        commandManager.register(new EchoCommand());
////        commandManager.register(new SummonCommand());
////        commandManager.register(new SummonRandomCommand());
////        commandManager.register(new RemoveCommand());
////        commandManager.register(new GiveCommand());
////        commandManager.register(new SetBlockCommand());
////        commandManager.register(new AutoViewCommand());
////        commandManager.register(new SaveCommand());
////        commandManager.register(new GamemodeCommand());
////        commandManager.register(new ExecuteCommand());
////        commandManager.register(new RedirectTestCommand());
////        commandManager.register(new DebugGridCommand());
////        commandManager.register(new DisplayCommand());
////        commandManager.register(new NotificationCommand());
////        commandManager.register(new TestCommand2());
////        commandManager.register(new ConfigCommand());
////        commandManager.register(new SidebarCommand());
////        commandManager.register(new SetEntityType());
////        commandManager.register(new RelightCommand());
////        commandManager.register(new KillCommand());
////        commandManager.register(new WeatherCommand());
////        commandManager.register(new PotionCommand());
////        commandManager.register(new CookieCommand());
////        commandManager.register(new WorldBorderCommand());
////        commandManager.register(new TransferCommand());
////        commandManager.register(new TestInstabreakCommand());
////        commandManager.register(new AttributeCommand());
////        commandManager.register(new PrimedTNTCommand());
////        commandManager.register(new SleepCommand());
////        commandManager.register(new MinecartCommand());
////        commandManager.register(new BelowNameCommand());
////        commandManager.register(new InventoryCommand());
////        commandManager.register(new SweptIntersectionCommand());
////        commandManager.register(new HeightmapCommand());
////
////        commandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage(Component.text("Unknown command", NamedTextColor.RED)));
////
////        new PlayerInit().init();
////
////        minecraftServer.start("0.0.0.0", 25565);
////    }
}