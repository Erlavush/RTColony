package com.erlavush.rtcolony.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class RtsCameraState {
    private static final float DEFAULT_YAW = 45.0F;
    private static final float DEFAULT_PITCH = 60.0F;
    private static final float DEFAULT_DISTANCE = 48.0F;
    private static final float MIN_DISTANCE = 14.0F;
    private static final float MAX_DISTANCE = 128.0F;
    private static final float DRAG_ROTATE_DEGREES_PER_PIXEL = 0.18F;
    private static final float DRAG_ORBIT_PITCH_DEGREES_PER_PIXEL = 0.16F;
    private static final float MIN_ORBIT_PITCH = -65.0F;
    private static final float MAX_ORBIT_PITCH = 85.0F;
    private static final float ZOOM_STEP = 4.0F;
    private static final double FLOOR_CLEARANCE = 0.75D;
    private static final double RENDER_SMOOTHING_PER_SECOND = 24.0D;

    private static boolean active;
    private static double targetCenterX;
    private static double targetCenterY;
    private static double targetCenterZ;
    private static float targetYaw = DEFAULT_YAW;
    private static float targetPitch = DEFAULT_PITCH;
    private static float targetDistance = DEFAULT_DISTANCE;
    private static double renderCenterX;
    private static double renderCenterY;
    private static double renderCenterZ;
    private static float renderYaw = DEFAULT_YAW;
    private static float renderPitch = DEFAULT_PITCH;
    private static float renderDistance = DEFAULT_DISTANCE;
    private static long lastRenderNanos;

    private RtsCameraState() {
    }

    public static void activateFromPlayer(LocalPlayer player) {
        if (player != null) {
            targetCenterX = player.getX();
            targetCenterY = player.getY() + 1.0D;
            targetCenterZ = player.getZ();
        }
        targetYaw = DEFAULT_YAW;
        targetPitch = DEFAULT_PITCH;
        targetDistance = DEFAULT_DISTANCE;
        resetRenderState();
        active = player != null;
    }

    public static void ensureActive(LocalPlayer player) {
        if (!active) {
            activateFromPlayer(player);
        }
    }

    public static void deactivate() {
        active = false;
        lastRenderNanos = 0L;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getYaw() {
        return renderYaw;
    }

    public static float getPitch() {
        return renderPitch;
    }

    public static float getDistance() {
        return renderDistance;
    }

    public static Vec3 getCenter() {
        return new Vec3(renderCenterX, renderCenterY, renderCenterZ);
    }

    public static Vec3 getCameraPosition() {
        Vec3 lookDirection = Vec3.directionFromRotation(renderPitch, renderYaw).normalize();
        return getCenter().subtract(lookDirection.scale(renderDistance));
    }

    public static Vec3 getPlacementCameraPosition(BlockGetter level) {
        Vec3 center = getCenter();
        Vec3 lookDirection = Vec3.directionFromRotation(renderPitch, renderYaw).normalize();
        Vec3 desiredPosition = center.subtract(lookDirection.scale(renderDistance));
        if (!(level instanceof ClientLevel clientLevel)) {
            return desiredPosition;
        }

        Vec3 centerToCamera = desiredPosition.subtract(center);
        double desiredDistance = centerToCamera.length();
        if (desiredDistance <= 0.0D) {
            return desiredPosition;
        }

        Vec3 orbitDirection = centerToCamera.normalize();
        double cameraDistance = floorClampedCameraDistance(clientLevel, center, desiredPosition, orbitDirection, desiredDistance);
        return center.add(orbitDirection.scale(cameraDistance));
    }

    public static void focusOn(Vec3 center) {
        if (center == null) {
            return;
        }

        targetCenterX = center.x;
        targetCenterY = center.y;
        targetCenterZ = center.z;
    }

    public static void shiftFocus(double deltaX, double deltaY, double deltaZ) {
        targetCenterX += deltaX;
        targetCenterY += deltaY;
        targetCenterZ += deltaZ;
    }

    public static void returnToRtsView() {
        targetPitch = DEFAULT_PITCH;
        renderPitch = DEFAULT_PITCH;
    }

    public static void advanceRenderState() {
        if (!active) {
            return;
        }

        long now = System.nanoTime();
        double elapsedSeconds = lastRenderNanos == 0L
                ? 1.0D / 60.0D
                : Mth.clamp((now - lastRenderNanos) / 1_000_000_000.0D, 0.0D, 0.1D);
        lastRenderNanos = now;

        double alpha = 1.0D - Math.exp(-RENDER_SMOOTHING_PER_SECOND * elapsedSeconds);
        renderCenterX = Mth.lerp(alpha, renderCenterX, targetCenterX);
        renderCenterY = Mth.lerp(alpha, renderCenterY, targetCenterY);
        renderCenterZ = Mth.lerp(alpha, renderCenterZ, targetCenterZ);
        renderYaw = Mth.rotLerp((float) alpha, renderYaw, targetYaw);
        renderPitch = Mth.lerp((float) alpha, renderPitch, targetPitch);
        renderDistance = Mth.lerp((float) alpha, renderDistance, targetDistance);
    }

    public static void zoom(double scrollDelta) {
        targetDistance = Mth.clamp((float) (targetDistance - scrollDelta * ZOOM_STEP), MIN_DISTANCE, MAX_DISTANCE);
    }

    public static void pan(float leftImpulse, float forwardImpulse) {
        if (leftImpulse == 0.0F && forwardImpulse == 0.0F) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, targetYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        double speed = Mth.clamp(targetDistance / 48.0D, 0.35D, 2.5D);
        Vec3 delta = forward.scale(forwardImpulse * speed).add(left.scale(leftImpulse * speed));
        targetCenterX += delta.x;
        targetCenterZ += delta.z;
    }

    public static void panFromScreenDrag(double deltaX, double deltaY) {
        if (!active || deltaX == 0.0D && deltaY == 0.0D) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, targetYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        double worldUnitsPerPixel = Mth.clamp(targetDistance / 700.0D, 0.02D, 0.18D);
        Vec3 delta = forward.scale(deltaY * worldUnitsPerPixel).add(left.scale(deltaX * worldUnitsPerPixel));
        targetCenterX += delta.x;
        targetCenterZ += delta.z;
    }

    public static void orbitLockedPlacementFromScreenDrag(
            double deltaX,
            double deltaY,
            boolean invertHorizontal,
            boolean invertVertical
    ) {
        if (!active || deltaX == 0.0D && deltaY == 0.0D) {
            return;
        }

        double adjustedDeltaX = invertHorizontal ? -deltaX : deltaX;
        double adjustedDeltaY = invertVertical ? -deltaY : deltaY;
        targetYaw = Mth.wrapDegrees((float) (targetYaw + adjustedDeltaX * DRAG_ROTATE_DEGREES_PER_PIXEL));
        targetPitch = Mth.clamp(
                (float) (targetPitch - adjustedDeltaY * DRAG_ORBIT_PITCH_DEGREES_PER_PIXEL),
                MIN_ORBIT_PITCH,
                MAX_ORBIT_PITCH
        );
    }

    public static void rotateFromScreenDrag(double deltaX) {
        if (!active || deltaX == 0.0D) {
            return;
        }

        targetYaw = Mth.wrapDegrees((float) (targetYaw + deltaX * DRAG_ROTATE_DEGREES_PER_PIXEL));
    }

    public static void updateTerrainHeight(ClientLevel level) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(targetCenterX), Mth.floor(targetCenterZ));
        double targetY = height + 1.0D;
        targetCenterY = Mth.lerp(0.12D, targetCenterY, targetY);
    }

    private static double floorClampedCameraDistance(
            ClientLevel level,
            Vec3 center,
            Vec3 desiredPosition,
            Vec3 orbitDirection,
            double desiredDistance
    ) {
        double floorY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                Mth.floor(desiredPosition.x),
                Mth.floor(desiredPosition.z)
        ) + FLOOR_CLEARANCE;
        if (desiredPosition.y >= floorY || orbitDirection.y >= 0.0D) {
            return desiredDistance;
        }

        double floorDistance = (floorY - center.y) / orbitDirection.y;
        return Mth.clamp(floorDistance, 0.0D, desiredDistance);
    }

    private static void resetRenderState() {
        renderCenterX = targetCenterX;
        renderCenterY = targetCenterY;
        renderCenterZ = targetCenterZ;
        renderYaw = targetYaw;
        renderPitch = targetPitch;
        renderDistance = targetDistance;
        lastRenderNanos = 0L;
    }
}
