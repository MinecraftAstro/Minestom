package net.minestom.demo.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.golem.ShulkerMeta;
import net.minestom.server.instance.Instance;

public class TestCommand extends Command {

    public TestCommand() {
        super("testcmd");

        setDefaultExecutor((sender, command) -> {
            final Player player = (Player) sender;

            final Instance instance = player.getInstance();

            final LivingEntity shulker = new LivingEntity(EntityType.SHULKER);
            shulker.setGlowing(true);
            shulker.setInvisible(true);
            shulker.setNoGravity(true);
            shulker.getAttribute(Attribute.SCALE).setBaseValue(5);
            shulker.setInstance(instance, player.getPosition());
        });
    }
}