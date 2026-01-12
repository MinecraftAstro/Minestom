package net.minestom.demo.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.particle.Particle;
import net.minestom.server.pathfinding.Pathfinder;
import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import net.minestom.server.pathfinding.utils.PathUtils;
import net.minestom.server.pathfinding.validation.types.FastNodeValidator;

public class ComeCommand extends Command {

    public static final Pathfinder PATHFINDER = new Pathfinder(
            new PathfinderOptions.Builder()
                    .nodeValidator(new FastNodeValidator())
                    .build()
    );

    public ComeCommand() {
        super("come");
        setCondition(Conditions::playerOnly);

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            final Instance currentInstance = player.getInstance();

            int successAmount = 0;
            long startTime = System.currentTimeMillis();
            for (Entity entity : currentInstance.getEntities()) {
                // only EntityCreatures can pathfind
                if (!(entity instanceof EntityMob entityMob))
                    continue;

                final Path path = PATHFINDER.findPath(
                        entity.getPosition(),
                        player.getPosition(),
                        new MobContext(currentInstance, entity.getBoundingBox(), 3),
                        1.0D
                ).join();

                System.out.println("Path Size: " + path.list().size());

//                entityMob.setPath(player.getPosition(), 1.0D, () ->
//                        player.sendMessage("Finished pathing."));

                if(path.state() == Path.State.FOUND) {
                    successAmount++;
                }

//                System.out.println(path.state());
//                System.out.println(path.start());
//                System.out.println(path.end());
//                System.out.println(path.collect().size());

                PathUtils.drawPath(path, Particle.COMPOSTER);

                // TODO: pathfinding
                // TODO: debug information
            }
            long endTime = System.currentTimeMillis();

            System.out.println("Took " + (endTime - startTime) + "ms to find " + successAmount + " paths.");
        });
    }
}