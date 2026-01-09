package net.minestom.demo.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;

public class AroundCommand extends Command {

    public AroundCommand() {
        super("around");
        setCondition(Conditions::playerOnly);

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            //final NodeGenerator nodeGenerator = new WalkNodeGenerator();
            //nodeGenerator.getValidNeighbors(new Node(player.getPosition(), 0.0D, 0.0D), player.getInstance(), player.getBoundingBox());
        });
    }
}