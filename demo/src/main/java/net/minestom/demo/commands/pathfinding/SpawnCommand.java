package net.minestom.demo.commands.pathfinding;

import net.kyori.adventure.text.Component;
import net.minestom.demo.entity.TestMob;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnCommand extends Command {

    private final ArgumentInteger amountArgument = new ArgumentInteger("amount");

    public SpawnCommand() {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            spawnMobs(player, 1);
            sender.sendMessage(Component.text("Spawned a mob."));
        });

        addSyntax((sender, context) -> {
            final Player player = (Player) sender;
            final int amount = context.get(amountArgument);
            spawnMobs(player, amount);
            sender.sendMessage(Component.text("Spawned " + amount + " mob(s)."));
        }, amountArgument.setDefaultValue(1));
    }

    private void spawnMobs(@NotNull Player player,
                           int amount) {
        if (amount == 1) {
            new TestMob().setInstance(player.getInstance(), player.getPosition()).join();
        } else {
            for (int i = 0; i < amount; i++) {
                final Pos spawnPos = new Pos(
                        ThreadLocalRandom.current().nextInt(0, 200),
                        40,
                        ThreadLocalRandom.current().nextInt(0, 200)
                );
                new TestMob().setInstance(player.getInstance(), spawnPos).join();
            }
        }
    }
}