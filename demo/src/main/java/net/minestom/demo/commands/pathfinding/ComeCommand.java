package net.minestom.demo.commands.pathfinding;

import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.MobEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.pathfinding.nodes.generator.NodeGenerator;
import net.minestom.server.entity.pathfinding.path.generator.PathGenerator;

public class ComeCommand extends Command {

    public ComeCommand() {
        super("come");

        setDefaultExecutor((sender, args) -> {
            final Player player = (Player) sender;

            //long start = System.currentTimeMillis();
            for (Entity entity : player.getInstance().getEntities()) {
                if (!(entity instanceof MobEntity mob))
                    continue;

                PathGenerator.generatePath(
                        mob,
                        new NodeGenerator(),
                        mob.getPosition(),
                        player.getPosition()
                ).whenComplete((path, exception) -> {
                    if(exception != null) {
                        System.out.println(exception.getMessage());
                        return;
                    }

                    System.out.println("Done with path");
                });
            }
            //long end = System.currentTimeMillis();

            //System.out.println("Took " + (end - start) + "ms to generate paths for all mobs.");
        });
    }
}