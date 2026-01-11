package net.minestom.server.entity.path.types;

import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.path.PathFollower;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GroundPathFollower extends PathFollower {

    private final EntityMob entityMob;

    private final List<Point> points;
    private int currentIndex;

    public GroundPathFollower(@NotNull EntityMob entityMob,
                              @NotNull Path path) {
        this.entityMob = entityMob;

        this.points = path.list();
        this.currentIndex = 0;
    }

    public void followPath() {
        final Pos position = entityMob.getPosition();

        // check if we're on the last path point so we can get as close to it as possible
        // this helps prevent issues where the completion callback is never called
        final Point currentPoint = points.get(currentIndex);
        final Point nextPoint = points.get(currentIndex + 1);
        if (currentIndex == points.size() - 2) {
            if (position.manhattanDistance(nextPoint) <= 0.01) {
                // this should be small enough
                return;
            }
        } else {
            if (position.sameBlock(currentPoint)) {
                currentIndex++;
            }
        }

        final double dx = currentPoint.x() - position.x();
        final double dy = currentPoint.y() - position.y();
        final double dz = currentPoint.z() - position.z();

        final double dxLook = nextPoint.x() - position.x();
        final double dyLook = nextPoint.y() - position.y();
        final double dzLook = nextPoint.z() - position.z();

        // slow down the entity when it is about to reach its destination
        double movementSpeed = entityMob.getAttribute(Attribute.MOVEMENT_SPEED).getValue();
        final double distSquared = dx * dx + dy * dy + dz * dz;
        if (movementSpeed > distSquared) {
            movementSpeed = distSquared;
        }

        final double radians = Math.atan2(dz, dx);
        final double speedX = Math.cos(radians) * movementSpeed;
        final double speedZ = Math.sin(radians) * movementSpeed;
        final float yaw = PositionUtils.getLookYaw(dxLook, dzLook);
        final float pitch = PositionUtils.getLookPitch(dxLook, dyLook, dzLook);

        final PhysicsResult physicsResult = CollisionUtils.handlePhysics(entityMob, new Vec(speedX, 0, speedZ));
        entityMob.refreshPosition(physicsResult.newPosition().asPos().withView(yaw, pitch));
    }
}