package net.minestom.demo.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.golem.ShulkerMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;

public class TestCommand extends Command {

    public TestCommand() {
        super("testcmd");

        setDefaultExecutor((sender, command) -> {
            final Player player = (Player) sender;

            final Instance instance = player.getInstance();

            final Entity entity = new Entity(EntityType.BLAZE);
            entity.setInstance(instance, player.getPosition());

            final Entity entity2 = new Entity(EntityType.CHICKEN);
            entity2.setInstance(instance, player.getPosition());

            entity2.addPassenger(entity);

            entity.setAutoViewable(false);
            entity.setAutoViewable(true);

            MinecraftServer.getSchedulerManager().buildTask(() -> {
                entity2.teleport(player.getPosition());

                System.out.println(entity2.getPosition());
                System.out.println(entity.getPosition());
            }).delay(TaskSchedule.seconds(3L)).schedule();
        });
    }
}