package net.minestom.demo.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public class ComeCommand extends Command {

    public ComeCommand() {
        super("come");
        setCondition(Conditions::playerOnly);

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            final Instance currentInstance = player.getInstance();

            for (Entity entity : currentInstance.getEntities()) {
                // only EntityCreatures can pathfind
                if (!(entity instanceof EntityMob))
                    continue;

                // TODO: pathfinding
                // TODO: debug information
            }
        });
    }
}