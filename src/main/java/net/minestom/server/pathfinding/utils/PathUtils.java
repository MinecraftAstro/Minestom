package net.minestom.server.pathfinding.utils;

import com.google.common.hash.BloomFilter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.Shape;
import net.minestom.server.collision.SweepResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public final class PathUtils {

    private PathUtils() {
    }

    public static void drawPath(@NotNull Path path,
                                @NotNull Particle particle) {
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            for(Point pathPoint : path) {
                final ParticlePacket particlePacket = new ParticlePacket(Particle.COMPOSTER, pathPoint.blockX() + 0.5, pathPoint.blockY() + 0.5, pathPoint.blockZ() + 0.5, 0, 0, 0, 0, 1);
                MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                    player.sendPacket(particlePacket);
                });
            }
        }, TaskSchedule.tick(2), TaskSchedule.tick(2));
    }

    // TODO: only for moving 1 node at a time
    public static boolean isPathClear(@NotNull Block.Getter context,
                                      @NotNull Vec startVec,
                                      @NotNull Vec endVec,
                                      @NotNull BoundingBox boundingBox) {
        final Vec direction = endVec.sub(startVec);
        //System.out.println("Direction: " + direction);
        final boolean diagonal = isDiagonalMove(direction);
        //System.out.println("Diagonal: " + diagonal);

        final SweepResult sweepResult = new SweepResult(Double.MAX_VALUE, 0, 0, 0, null, 0, 0, 0, 0, 0, 0);
        if (!diagonal) {
            // non-diagonal moves require fewer checks than a diagonal move since we don't have to worry about neighboring blocks
            System.out.println("Checking non-diagonal move");

            // TODO: for a player-sized bounding box, a straight move should result in 4 iterations if they are standing in the middle
            final int width = (int) Math.ceil(boundingBox.width());
            final int height = (int) Math.ceil(boundingBox.height());

            System.out.println("Bounding Box Width: " + width);
            System.out.println("Bounding Box Height: " + height);

            final int minBlockX = Math.min(startVec.blockX(), endVec.blockX());
            final int minBlockY = Math.min(startVec.blockY(), endVec.blockY());
            final int minBlockZ = Math.min(startVec.blockZ(), endVec.blockZ());
            final int maxBlockX = Math.max(startVec.blockX(), endVec.blockX());
            final int maxBlockY = Math.max(startVec.blockY(), endVec.blockY());
            final int maxBlockZ = Math.max(startVec.blockZ(), endVec.blockZ());

            for (int x = minBlockX; x <= maxBlockX; x++) {
                for (int y = minBlockY; y < maxBlockY + height; y++) {
                    for (int z = minBlockZ; z <= maxBlockZ; z++) {
                        int finalX = x;
                        int finalY = y;
                        int finalZ = z;

                        // TODO: do we even need to worry about collisions that can be stepped over (0.6 default step height)
                        final Block block = context.getBlock(x, y, z, Block.Getter.Condition.TYPE);
                        final Shape shape = block.registry().collisionShape();
                        if (shape.intersectBoxSwept(startVec, direction, new Vec(x, y, z), boundingBox, sweepResult)) {
                            return false;
                        }

                        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                            final ParticlePacket particlePacket = new ParticlePacket(Particle.COMPOSTER, finalX + 0.5, finalY + 0.5, finalZ + 0.5, 0, 0, 0, 0, 1);
                            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                                player.sendPacket(particlePacket);
                            });
                        }, TaskSchedule.tick(2), TaskSchedule.tick(2));
                    }
                }
            }
        } else {
            // we have to check the neighbor nodes now
        }

        return true;
    }

    public static boolean isDiagonalMove(@NotNull Vec direction) {
        return Math.abs(direction.blockX()) == Math.abs(direction.blockZ());
    }
}