package net.minestom.server.entity.path;

import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.pathfinding.data.Path;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;

public final class GroundPathFollower {

    private final EntityMob entityMob;

    private Point currentPoint;
    private Point nextPoint;

    public GroundPathFollower(@NotNull EntityMob entityMob,
                              @NotNull Path path) {
        this.entityMob = entityMob;

        // establish the initial current point and next point
        final int pathLength = path.length();
        if (pathLength == 0) {
            currentPoint = null;
            nextPoint = null;
        } else if (pathLength == 1) {
            currentPoint = path.start();
            nextPoint = path.end();
        } else {
            currentPoint = path.start();
            nextPoint = path.iterator().next();
        }
    }

    public void followPath() {
        final Pos position = entityMob.getPosition();

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