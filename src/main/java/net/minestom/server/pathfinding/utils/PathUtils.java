package net.minestom.server.pathfinding.utils;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public final class PathUtils {

    private PathUtils() {
    }

    public static void drawPath(@NotNull Path path,
                                @NotNull Particle particle) {
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            for (Point pathPoint : path) {
                final ParticlePacket particlePacket = new ParticlePacket(particle, pathPoint.blockX() + 0.5, pathPoint.y() + 0.5, pathPoint.blockZ() + 0.5, 0, 0, 0, 0, 1);
                MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                    player.sendPacket(particlePacket);
                });
            }
        }, TaskSchedule.tick(2), TaskSchedule.tick(2));
    }
}