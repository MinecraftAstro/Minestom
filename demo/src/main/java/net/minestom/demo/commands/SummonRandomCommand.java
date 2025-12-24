package net.minestom.demo.commands;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentEntityType;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.instance.block.Block;

import java.util.Random;

public class SummonRandomCommand extends Command {

    private final Random random = new Random();

    private final ArgumentEntityType entity;
    private final ArgumentInteger squareRadius;
    private final Argument<SummonCommand.EntityClass> entityClass;
    private final ArgumentInteger amount;

    public SummonRandomCommand() {
        super("summonrandom");
        setCondition(Conditions::playerOnly);

        entity = ArgumentType.EntityType("entity type");
        squareRadius = ArgumentType.Integer("radius");
        entityClass = ArgumentType.Enum("class", SummonCommand.EntityClass.class)
                .setFormat(ArgumentEnum.Format.LOWER_CASED)
                .setDefaultValue(SummonCommand.EntityClass.CREATURE);
        amount = ArgumentType.Integer("amount");

        addSyntax(this::execute, entity, squareRadius, entityClass, amount);
        setDefaultExecutor((sender, context) ->
                sender.sendMessage("Usage: /summon <type> <radius> <class> <amount>"));
    }

    private void execute(CommandSender commandSender, CommandContext commandContext) {
        final Player player = (Player) commandSender;

        final int squareRadiusValue = commandContext.get(squareRadius);
        final int amountValue = commandContext.get(amount);

        final Pos playerPosition = player.getPosition();

        for (int i = 0; i < amountValue; i++) {
            final int randomX = random.nextInt(playerPosition.blockX() - squareRadiusValue, playerPosition.blockX() + squareRadiusValue);
            final int randomZ = random.nextInt(playerPosition.blockZ() - squareRadiusValue, playerPosition.blockZ() + squareRadiusValue);

            long highestBlockY = 0;
            for (int y = 0; y < 320; y++) {
                final Block block = player.getInstance().getBlock(randomX, y, randomZ);
                if (block == Block.AIR || block == Block.VOID_AIR || block == Block.CAVE_AIR) {
                    continue;
                }

                highestBlockY = y;
            }

            final Pos spawnPos = new Pos(randomX, highestBlockY + 1, randomZ);

            final Entity entity = commandContext.get(entityClass).instantiate(commandContext.get(this.entity));
            entity.setInstance(((Player) commandSender).getInstance(), spawnPos);
        }
    }

    @SuppressWarnings("unused")
    enum EntityClass {
        BASE(Entity::new),
        LIVING(LivingEntity::new),
        CREATURE(EntityMob::new);
        private final EntityFactory factory;

        EntityClass(EntityFactory factory) {
            this.factory = factory;
        }

        public Entity instantiate(EntityType type) {
            return factory.newInstance(type);
        }
    }

    interface EntityFactory {
        Entity newInstance(EntityType type);
    }
}