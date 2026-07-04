package com.erlavush.rtcolony.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class RtsCameraState {
    private static final float DEFAULT_YAW = 45.0F;
    private static final float DEFAULT_PITCH = 60.0F;
    private static final float DEFAULT_DISTANCE = 48.0F;
    private static final float MIN_DISTANCE = 14.0F;
    private static final float MAX_DISTANCE = 128.0F;
    private static final float ROTATE_DEGREES_PER_TICK = 2.5F;
    private static final float ZOOM_STEP = 4.0F;

    private static boolean active;
    private static double centerX;
    private static double centerY;
    private static double centerZ;
    private static float yaw = DEFAULT_YAW;
    private static float pitch = DEFAULT_PITCH;
    private static float distance = DEFAULT_DISTANCE;

    private RtsCameraState() {
    }

    public static void activateFromPlayer(LocalPlayer player) {
        if (player != null) {
            centerX = player.getX();
            centerY = player.getY() + 1.0D;
            centerZ = player.getZ();
        }
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        distance = DEFAULT_DISTANCE;
        active = player != null;
    }

    public static void ensureActive(LocalPlayer player) {
        if (!active) {
            activateFromPlayer(player);
        }
    }

    public static void deactivate() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    public static float getDistance() {
        return distance;
    }

    public static Vec3 getCenter() {
        return new Vec3(centerX, centerY, centerZ);
    }

    public static Vec3 getCameraPosition() {
        Vec3 lookDirection = Vec3.directionFromRotation(pitch, yaw).normalize();
        return getCenter().subtract(lookDirection.scale(distance));
    }

    public static void rotateLeft() {
        yaw = Mth.wrapDegrees(yaw - ROTATE_DEGREES_PER_TICK);
    }

    public static void rotateRight() {
        yaw = Mth.wrapDegrees(yaw + ROTATE_DEGREES_PER_TICK);
    }

    public static void zoom(double scrollDelta) {
        distance = Mth.clamp((float) (distance - scrollDelta * ZOOM_STEP), MIN_DISTANCE, MAX_DISTANCE);
    }

    public static void pan(float leftImpulse, float forwardImpulse) {
        if (leftImpulse == 0.0F && forwardImpulse == 0.0F) {
            return;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        double speed = Mth.clamp(distance / 48.0D, 0.35D, 2.5D);
        Vec3 delta = forward.scale(forwardImpulse * speed).add(left.scale(leftImpulse * speed));
        centerX += delta.x;
        centerZ += delta.z;
    }

    public static void updateTerrainHeight(ClientLevel level) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(centerX), Mth.floor(centerZ));
        double targetY = height + 1.0D;
        centerY = Mth.lerp(0.12D, centerY, targetY);
    }
}
