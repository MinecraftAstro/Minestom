package net.minestom.demo.commands;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;

public class AroundCommand extends Command {

    public AroundCommand() {
        super("around");
        setCondition(Conditions::playerOnly);

        setDefaultExecutor((sender, context) -> {
            final Player player = (Player) sender;
            player.sendMessage("Has Clearance: " + hasClearance(player));
            //final NodeGenerator nodeGenerator = new WalkNodeGenerator();
            //nodeGenerator.getValidNeighbors(new Node(player.getPosition(), 0.0D, 0.0D), player.getInstance(), player.getBoundingBox());
        });
    }

    private boolean hasClearance(Player player) {
        final BoundingBox.PointIterator blockIterator = player.getBoundingBox().getBlocks(player.getPosition());
        while (blockIterator.hasNext()) {
            final BoundingBox.MutablePoint blockPoint = blockIterator.next();
            final Block block = player.getInstance().getBlock(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ(), Block.Getter.Condition.TYPE);

            if (block == null) continue;
            if (block.id() == Block.SCAFFOLDING.id()) continue;

            final boolean hit = block.registry().collisionShape().intersectBox(player.getPosition().sub(blockPoint.blockX(), blockPoint.blockY(), blockPoint.blockZ()), player.getBoundingBox());
            if (hit) return false;
        }

        return true;
    }
}