package net.minestom.server.entity.ai.navigation.types;

import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityMob;
import net.minestom.server.entity.ai.navigation.PathNavigator;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.pathfinding.Pathfinder;
import net.minestom.server.pathfinding.data.PathPoint;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;

public final class GroundPathNavigator extends PathNavigator {

    private static final double MIN_WAYPOINT_RANGE = 0.1D;
    private static final double MOVEMENT_EPSILON_SQUARED = 1.0E-8;
    private static final double LOOK_EPSILON_SQUARED = 1.0E-8;
    private static final double STEP_VERTICAL_SPEED = 0.8D;
    private static final double JUMP_VERTICAL_SPEED = 10.0D;
    private static final double JUMP_TRIGGER_DISTANCE_SQUARED = 1.5D * 1.5D;
    private static final double VERTICAL_EPSILON = 1.0E-3;

    public GroundPathNavigator(@NotNull EntityMob entityMob,
                               @NotNull Pathfinder pathfinder) {
        super(entityMob, pathfinder);
    }

    public GroundPathNavigator(@NotNull EntityMob entityMob) {
        super(entityMob);
    }

    @Override
    public void navigatePath() {
        // can't navigate an empty path
        if (pathPoints.isEmpty())
            return;

        final double movementSpeed = entityMob.getAttributeValue(Attribute.MOVEMENT_SPEED);
        if (movementSpeed <= 0) {
            return;
        }

        advancePathIndex(movementSpeed);

        final PathPoint pathPoint = pathPoints.get(currentIndex);
        final PathPoint nextPathPoint;
        if (currentIndex >= pathPoints.size() - 1) {
            nextPathPoint = pathPoint;
        } else {
            nextPathPoint = pathPoints.get(currentIndex + 1);
        }

        handleMove(pathPoint, nextPathPoint, movementSpeed);
    }

    private void advancePathIndex(double movementSpeed) {
        final double waypointRange = Math.max(movementSpeed, MIN_WAYPOINT_RANGE);
        final double waypointRangeSquared = waypointRange * waypointRange;

        while (currentIndex < pathPoints.size() - 1) {
            final Pos entityPosition = entityMob.getPosition();
            final Point currentPoint = pathPoints.get(currentIndex).point();
            final double targetX = getBlockCenterX(currentPoint);
            final double targetZ = getBlockCenterZ(currentPoint);

            final double dx = targetX - entityPosition.x();
            final double dz = targetZ - entityPosition.z();
            final double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > waypointRangeSquared) {
                break;
            }

            currentIndex++;
        }
    }

    private void handleMove(PathPoint pathPoint,
                            PathPoint nextPathPoint,
                            double movementSpeed) {
        switch (pathPoint.type()) {
            case STEP -> stepTo(pathPoint.point(), nextPathPoint.point(), movementSpeed);
            case JUMP -> jumpTo(pathPoint.point(), nextPathPoint.point(), movementSpeed);
            default -> moveTo(pathPoint.point(), nextPathPoint.point(), movementSpeed, 0.0D);
        }
    }

    private void stepTo(Point point,
                        Point nextPoint,
                        double movementSpeed) {
        final Pos entityPosition = entityMob.getPosition();
        final double heightDelta = point.y() - entityPosition.y();
        System.out.println("Height delta: " + heightDelta);
        final double verticalSpeed = heightDelta > VERTICAL_EPSILON ? Math.min(heightDelta + 0.1D, STEP_VERTICAL_SPEED) : 0.0D;

        moveTo(point, nextPoint, movementSpeed, verticalSpeed);
    }

    private void jumpTo(Point point,
                        Point nextPoint,
                        double movementSpeed) {
        if (entityMob.isOnGround()) {
            final Pos entityPosition = entityMob.getPosition();
            final double heightDelta = point.y() - entityPosition.y();
            if (heightDelta > VERTICAL_EPSILON) {
                final double dx = getBlockCenterX(point) - entityPosition.x();
                final double dz = getBlockCenterZ(point) - entityPosition.z();
                final double distanceSquared = dx * dx + dz * dz;
                if (distanceSquared <= JUMP_TRIGGER_DISTANCE_SQUARED) {
                    final Vec velocity = entityMob.getVelocity();
                    if (velocity.y() < JUMP_VERTICAL_SPEED) {
                        entityMob.setVelocity(velocity.withY(JUMP_VERTICAL_SPEED));
                    }
                }
            }
        }

        moveTo(point, nextPoint, movementSpeed, 0.0D);
    }

    private void moveTo(Point point,
                        Point nextPoint,
                        double movementSpeed,
                        double verticalSpeed) {
        final Pos entityPosition = entityMob.getPosition();
        final double targetX = getBlockCenterX(point);
        final double targetZ = getBlockCenterZ(point);
        final double nextTargetX = getBlockCenterX(nextPoint);
        final double nextTargetZ = getBlockCenterZ(nextPoint);

        // get the difference between the point we need to get to and the entities current position
        final double dx = targetX - entityPosition.x();
        final double dy = point.y() - entityPosition.y();
        final double dz = targetZ - entityPosition.z();
        final double horizontalDistanceSquared = dx * dx + dz * dz;
        final boolean hasHorizontalMovement = horizontalDistanceSquared > MOVEMENT_EPSILON_SQUARED;
        final boolean hasVerticalMovement = Math.abs(verticalSpeed) > Vec.EPSILON;
        if (!hasHorizontalMovement && !hasVerticalMovement) {
            return;
        }

        // get the direction that the entity should look at, which will be the difference between the next point and the entities current position
        double dxLook = nextTargetX - entityPosition.x();
        double dyLook = nextPoint.y() - entityPosition.y();
        double dzLook = nextTargetZ - entityPosition.z();
        final double lookDistanceSquared = dxLook * dxLook + dyLook * dyLook + dzLook * dzLook;
        if (lookDistanceSquared <= LOOK_EPSILON_SQUARED) {
            dxLook = dx;
            dyLook = dy;
            dzLook = dz;
        }

        final double speedX;
        final double speedZ;
        if (hasHorizontalMovement) {
            final double horizontalDistance = Math.sqrt(horizontalDistanceSquared);
            final double clampedSpeed = Math.min(movementSpeed, horizontalDistance);
            final double speedScale = clampedSpeed / horizontalDistance;
            speedX = dx * speedScale;
            speedZ = dz * speedScale;
        } else {
            speedX = 0.0D;
            speedZ = 0.0D;
        }

        final float yaw = PositionUtils.getLookYaw(dxLook, dzLook);
        final float pitch = PositionUtils.getLookPitch(dxLook, dyLook, dzLook);

        // actually move the entity with respect for correct physics
        final PhysicsResult physicsResult = CollisionUtils.handlePhysics(entityMob, new Vec(speedX, verticalSpeed, speedZ));
        entityMob.refreshPosition(physicsResult.newPosition().asPos().withView(yaw, pitch));
    }

    private static double getBlockCenterX(Point point) {
        return point.blockX() + 0.5D;
    }

    private static double getBlockCenterZ(Point point) {
        return point.blockZ() + 0.5D;
    }
}