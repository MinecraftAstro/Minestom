package net.minestom.server.entity.ai.navigation.types;

import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.ai.navigation.PathNavigator;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.pathfinding.Pathfinder;
import net.minestom.server.pathfinding.data.PathPoint;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;

public final class GroundPathNavigator extends PathNavigator {

    public GroundPathNavigator(@NotNull EntityMob entityMob,
                               @NotNull Pathfinder pathfinder) {
        super(entityMob, pathfinder);
    }

    public GroundPathNavigator(@NotNull EntityMob entityMob) {
        super(entityMob);
    }

    @Override
    protected void navigatePath() {
        // can't navigate an empty path
        if (pathPoints.isEmpty())
            return;

        final double movementSpeed = entityMob.getAttributeValue(Attribute.MOVEMENT_SPEED);
        if (movementSpeed <= 0) {
            // can't move the mob if they have no movement speed
            return;
        }

        final int pathSize = pathPoints.size() - 1;

        // we need to make sure that the mob is close enough to the target point in the path before they move to the next one
        // there is a cap of the minimum completion distance just in-case movement speed is super slow
        final double minimumCompletionDistance = Math.max(movementSpeed, 0.1D);
        final double completionDistance = minimumCompletionDistance * minimumCompletionDistance;

        // try to advance the path point index
        // this happens when the mob is close enough to the next path point
        PathPoint targetPathPoint = pathPoints.get(currentIndex);
        while (currentIndex < pathSize) {
            final Pos entityPosition = entityMob.getPosition();

            // TODO: we can avoid extra computation if we shared the result of targetX and targetZ with the movement functions
            final Point targetPoint = targetPathPoint.point();
            final double targetX = targetPoint.centerBlockX();
            final double targetZ = targetPoint.centerBlockZ();

            final double dx = targetX - entityPosition.x();
            final double dz = targetZ - entityPosition.z();
            final double distanceFromTarget = dx * dx + dz * dz;

            // we aren't close enough to the target point to advance in the path points
            // we'll have to make the mob move closer to the target point and check again...
            if (distanceFromTarget > completionDistance) {
                break;
            }

            // the mob is close enough to the target point, advance to the next position
            targetPathPoint = pathPoints.get(++currentIndex);
        }

        // determine if we are at the end of the path so we don't access illegal indexes
        // the look at path point will be where the mob looks at
        final PathPoint lookAtPathPoint;
        if (currentIndex >= pathSize) {
            lookAtPathPoint = targetPathPoint;
        } else {
            lookAtPathPoint = pathPoints.get(currentIndex + 1);
        }

        // handle various different types of movement depending on the node's type
        switch(targetPathPoint.type()) {
            case STEP -> {

            }

            case JUMP -> {

            }

            case SWIM -> {

            }

            case FLOAT -> {
                // TODO: special case, keep the mob movement ticking so it remains floating
            }

            default -> {
                moveTo(targetPathPoint.point(), lookAtPathPoint.point());
            }
        }
    }

    private void moveTo(@NotNull Point targetPoint,
                        @NotNull Point lookAtPoint) {
        final Pos entityPosition = entityMob.getPosition();

        final double targetX = targetPoint.centerBlockX();
        final double targetZ = targetPoint.centerBlockZ();
        final double lookAtX = lookAtPoint.centerBlockX();
        final double lookAtZ = lookAtPoint.centerBlockZ();

        // get the difference between the point we need to get to and the entity's current position
        final double dx = targetX - entityPosition.x();
        final double dy = targetPoint.y() - entityPosition.y();
        final double dz = targetZ - entityPosition.z();

        // get the direction that the entity should look towards
        // this will be the difference between the look at point and the entity's current position
        final double dxLook = lookAtX - entityPosition.x();
        final double dyLook = lookAtPoint.y() - entityPosition.y();
        final double dzLook = lookAtZ - entityPosition.z();

        final float yaw = PositionUtils.getLookYaw(dxLook, dzLook);
        final float pitch = PositionUtils.getLookPitch(dxLook, dyLook, dzLook);

        // actually move the entity with respect to physics
        final PhysicsResult physicsResult = CollisionUtils.handlePhysics(entityMob, null);
        entityMob.refreshPosition(physicsResult.newPosition().asPos().withView(yaw, pitch));
    }
}