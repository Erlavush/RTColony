package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RtsCameraState {
    private static final float DEFAULT_YAW = 45.0F;
    private static final float DEFAULT_PITCH = 60.0F;
    private static final float FIXED_ANGLE_PITCH = 45.0F;
    private static final float ISOMETRIC_PITCH = 35.264F;
    private static final float DEFAULT_DISTANCE = 36.0F;
    private static final float MIN_DISTANCE = 8.0F;
    private static final float MAX_DISTANCE = 128.0F;
    private static final float DRAG_ROTATE_DEGREES_PER_PIXEL = 0.18F;
    private static final float DRAG_ORBIT_PITCH_DEGREES_PER_PIXEL = 0.16F;
    private static final float ISOMETRIC_ROTATE_DRAG_PIXELS = 28.0F;
    private static final float MIN_ORBIT_PITCH = -65.0F;
    private static final float MAX_ORBIT_PITCH = 85.0F;
    private static final float ZOOM_STEP = 4.0F;
    private static final double CAMERA_ALTITUDE_STEP = 4.0D;
    private static final double FLOOR_CLEARANCE = 0.75D;
    private static final double RENDER_SMOOTHING_PER_SECOND = 24.0D;
    private static final long FREECAM_RETURN_DURATION_NANOS = 700_000_000L;

    private static boolean active;
    private static RtsCameraMode mode = RtsCameraMode.PERSPECTIVE;
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
    private static double targetAbsoluteX;
    private static double targetAbsoluteY;
    private static double targetAbsoluteZ;
    private static double renderAbsoluteX;
    private static double renderAbsoluteY;
    private static double renderAbsoluteZ;
    private static boolean absolutePlacementOrbitActive;
    private static boolean absolutePlacementRestoreAvailable;
    private static double absoluteRestoreX;
    private static double absoluteRestoreY;
    private static double absoluteRestoreZ;
    private static double absoluteRestoreCenterX;
    private static double absoluteRestoreCenterY;
    private static double absoluteRestoreCenterZ;
    private static float absoluteRestoreYaw;
    private static float absoluteRestorePitch;
    private static float absoluteRestoreDistance;
    private static double terrainHeightOffset;
    private static boolean terrainHeightOffsetInitialized;
    private static long lastRenderNanos;
    private static double isometricDragAccumulator;
    private static boolean freecamReturnActive;
    private static long freecamReturnStartNanos;
    private static double freecamReturnSourceX;
    private static double freecamReturnSourceY;
    private static double freecamReturnSourceZ;
    private static double freecamReturnSourceCenterX;
    private static double freecamReturnSourceCenterY;
    private static double freecamReturnSourceCenterZ;
    private static float freecamReturnSourceYaw;
    private static float freecamReturnSourcePitch;

    private RtsCameraState() {
    }

    public static void activateFromPlayer(LocalPlayer player) {
        if (mode == RtsCameraMode.FREECAM) {
            mode = RtsCameraMode.PERSPECTIVE;
        }
        freecamReturnActive = false;
        if (player != null) {
            targetCenterX = player.getX();
            targetCenterY = player.getY() + 1.0D;
            targetCenterZ = player.getZ();
        }
        targetYaw = DEFAULT_YAW;
        targetPitch = pitchForMode();
        targetDistance = DEFAULT_DISTANCE;
        resetRenderState();
        resetAbsolutePositionFromOrbit();
        clearAbsolutePlacementRestore();
        terrainHeightOffsetInitialized = false;
        active = player != null;
    }

    public static void ensureActive(LocalPlayer player) {
        if (!active) {
            activateFromPlayer(player);
        }
    }

    public static void deactivate() {
        RtsFreecamIntegration.disable();
        if (mode == RtsCameraMode.FREECAM) {
            mode = RtsCameraMode.PERSPECTIVE;
        }
        freecamReturnActive = false;
        active = false;
        clearAbsolutePlacementRestore();
        terrainHeightOffsetInitialized = false;
        lastRenderNanos = 0L;
    }

    public static boolean isActive() {
        return active;
    }

    public static RtsCameraMode getMode() {
        return mode;
    }

    public static boolean isTrueIsometric() {
        return mode == RtsCameraMode.TRUE_ISOMETRIC;
    }

    public static boolean isFixedAngle() {
        return mode == RtsCameraMode.FIXED_ANGLE;
    }

    public static boolean isFreecam() {
        return mode == RtsCameraMode.FREECAM;
    }

    public static boolean isTransitioningFromFreecam() {
        return freecamReturnActive;
    }

    public static boolean ownsRtsCamera() {
        return !isFreecam();
    }

    public static boolean usesPerspectiveRig() {
        return mode == RtsCameraMode.PERSPECTIVE || mode == RtsCameraMode.FIXED_ANGLE;
    }

    public static boolean usesTerrainFollowing() {
        return mode == RtsCameraMode.PERSPECTIVE;
    }

    public static void cycleMode() {
        if (freecamReturnActive) {
            return;
        }

        if (mode.next() == RtsCameraMode.FREECAM && RtsBuildDrawer.isPreviewActive()) {
            setMode(RtsCameraMode.PERSPECTIVE);
            return;
        }
        setMode(mode.next());
    }

    public static void setMode(RtsCameraMode newMode) {
        if (newMode == null || RtsCameraState.mode == newMode || freecamReturnActive) {
            return;
        }

        if (newMode == RtsCameraMode.FREECAM) {
            enterFreecam();
            return;
        }

        if (isFreecam()) {
            beginFreecamReturn();
            return;
        }

        Vec3 previousCameraPosition = getCameraPosition();
        Vec3 previousCenter = getCenter();
        float previousYaw = renderYaw;
        double previousDistance = previousCameraPosition.distanceTo(previousCenter);
        if (previousDistance <= 1.0E-6D) {
            previousDistance = DEFAULT_DISTANCE;
        }
        RtsCameraState.mode = newMode;
        isometricDragAccumulator = 0.0D;

        if (usesPerspectiveRig()) {
            targetCenterX = previousCenter.x;
            targetCenterY = previousCenter.y;
            targetCenterZ = previousCenter.z;
            renderCenterX = targetCenterX;
            renderCenterY = targetCenterY;
            renderCenterZ = targetCenterZ;
            targetYaw = previousYaw;
            targetPitch = pitchForMode();
            targetDistance = (float) previousDistance;
            Vec3 lookDirection = Vec3.directionFromRotation(targetPitch, targetYaw).normalize();
            targetAbsoluteX = targetCenterX - lookDirection.x * previousDistance;
            targetAbsoluteY = targetCenterY - lookDirection.y * previousDistance;
            targetAbsoluteZ = targetCenterZ - lookDirection.z * previousDistance;
            renderAbsoluteX = targetAbsoluteX;
            renderAbsoluteY = targetAbsoluteY;
            renderAbsoluteZ = targetAbsoluteZ;
            updateAbsoluteTargetLookAt();
            updateAbsoluteRenderLookAt();
            absolutePlacementOrbitActive = RtsBuildDrawer.isPlacementLocked();
            if (absolutePlacementOrbitActive) {
                captureAbsolutePlacementRestore();
            } else {
                absolutePlacementRestoreAvailable = false;
            }
            terrainHeightOffsetInitialized = false;
            RTColony.LOGGER.info("RTS camera mode: {}", isFixedAngle() ? "fixed angle" : "perspective");
            return;
        }

        targetPitch = pitchForMode();
        targetYaw = snapToIsometricYaw(targetYaw);
        renderYaw = targetYaw;
        renderPitch = targetPitch;
        clearAbsolutePlacementRestore();
        terrainHeightOffsetInitialized = false;
        RTColony.LOGGER.info("RTS camera mode: true isometric");
    }

    public static void synchronizeFreecamState() {
        if (isFreecam() && !RtsFreecamIntegration.isEnabled()) {
            beginFreecamReturn(false);
        } else if (!isFreecam() && !freecamReturnActive && RtsFreecamIntegration.isEnabled()) {
            RtsFreecamIntegration.disable();
        }
    }

    public static float getYaw() {
        return renderYaw;
    }

    public static float getTargetYaw() {
        return targetYaw;
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
        if (isFreecam()) {
            return RtsFreecamIntegration.captureVisualPose().position();
        }

        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            return new Vec3(renderAbsoluteX, renderAbsoluteY, renderAbsoluteZ);
        }

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
        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            focusAbsoluteCameraOn(center);
            updateAbsoluteTargetLookAt();
            if (usesTerrainFollowing()) {
                terrainHeightOffsetInitialized = false;
            }
        }
    }

    public static void beginLockedPlacement(Vec3 center) {
        if (center == null) {
            return;
        }

        if (!usesPerspectiveRig() || absolutePlacementOrbitActive) {
            focusOn(center);
            return;
        }

        captureAbsolutePlacementRestore();
        absolutePlacementOrbitActive = true;

        Vec3 cameraPosition = new Vec3(renderAbsoluteX, renderAbsoluteY, renderAbsoluteZ);
        Vec3 lookOffset = center.subtract(cameraPosition);
        double distance = lookOffset.length();
        if (distance <= 1.0E-6D) {
            targetYaw = DEFAULT_YAW;
            targetPitch = DEFAULT_PITCH;
            distance = DEFAULT_DISTANCE;
        } else {
            targetYaw = yawFromDirection(lookOffset);
            targetPitch = Mth.clamp(pitchFromDirection(lookOffset), MIN_ORBIT_PITCH, MAX_ORBIT_PITCH);
        }

        targetCenterX = center.x;
        targetCenterY = center.y;
        targetCenterZ = center.z;
        renderCenterX = targetCenterX;
        renderCenterY = targetCenterY;
        renderCenterZ = targetCenterZ;
        targetDistance = Mth.clamp((float) distance, MIN_DISTANCE, MAX_DISTANCE);
        renderDistance = targetDistance;
        renderYaw = targetYaw;
        renderPitch = targetPitch;
    }

    public static void shiftFocus(double deltaX, double deltaY, double deltaZ) {
        RtsTargetingState.stopFollowing();
        targetCenterX += deltaX;
        targetCenterY += deltaY;
        targetCenterZ += deltaZ;
        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            targetAbsoluteX += deltaX;
            targetAbsoluteY += deltaY;
            targetAbsoluteZ += deltaZ;
        }
    }

    public static void returnToRtsView() {
        if (usesPerspectiveRig()) {
            restoreAbsoluteAfterPlacement();
            return;
        }

        clearAbsolutePlacementRestore();
        targetPitch = pitchForMode();
        renderPitch = targetPitch;
        if (isTrueIsometric()) {
            targetYaw = snapToIsometricYaw(targetYaw);
            renderYaw = targetYaw;
        }
    }

    public static void advanceRenderState() {
        if (!active) {
            return;
        }

        long now = System.nanoTime();
        if (freecamReturnActive) {
            advanceFreecamReturn(now);
            return;
        }

        if (isFreecam()) {
            return;
        }

        double elapsedSeconds = lastRenderNanos == 0L
                ? 1.0D / 60.0D
                : Mth.clamp((now - lastRenderNanos) / 1_000_000_000.0D, 0.0D, 0.1D);
        lastRenderNanos = now;

        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            renderCenterX = targetCenterX;
            renderCenterY = targetCenterY;
            renderCenterZ = targetCenterZ;
            renderAbsoluteX = targetAbsoluteX;
            renderAbsoluteY = targetAbsoluteY;
            renderAbsoluteZ = targetAbsoluteZ;
            updateAbsoluteRenderLookAt();
            return;
        }

        double alpha = 1.0D - Math.exp(-RENDER_SMOOTHING_PER_SECOND * elapsedSeconds);
        renderCenterX = Mth.lerp(alpha, renderCenterX, targetCenterX);
        renderCenterY = Mth.lerp(alpha, renderCenterY, targetCenterY);
        renderCenterZ = Mth.lerp(alpha, renderCenterZ, targetCenterZ);
        if (isTrueIsometric()) {
            renderYaw = targetYaw;
            renderPitch = targetPitch;
            renderDistance = Mth.lerp((float) alpha, renderDistance, targetDistance);
            return;
        }
        renderYaw = Mth.rotLerp((float) alpha, renderYaw, targetYaw);
        renderPitch = Mth.lerp((float) alpha, renderPitch, targetPitch);
        renderDistance = Mth.lerp((float) alpha, renderDistance, targetDistance);
    }

    public static void zoom(double scrollDelta) {
        if (scrollDelta == 0.0D) {
            return;
        }
        RtsTargetingState.stopFollowing();
        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            double altitudeDelta = -scrollDelta * CAMERA_ALTITUDE_STEP;
            targetAbsoluteX = renderAbsoluteX;
            targetAbsoluteY = renderAbsoluteY + altitudeDelta;
            targetAbsoluteZ = renderAbsoluteZ;
            targetCenterX = renderCenterX;
            targetCenterY = renderCenterY + altitudeDelta;
            targetCenterZ = renderCenterZ;
            renderCenterY = targetCenterY;
            renderAbsoluteY = targetAbsoluteY;
            if (usesTerrainFollowing() && terrainHeightOffsetInitialized) {
                terrainHeightOffset += altitudeDelta;
            }
            updateAbsoluteTargetLookAt();
            updateAbsoluteRenderLookAt();
            return;
        }
        targetDistance = Mth.clamp((float) (targetDistance - scrollDelta * ZOOM_STEP), MIN_DISTANCE, MAX_DISTANCE);
    }

    public static void pan(float leftImpulse, float forwardImpulse) {
        pan(leftImpulse, forwardImpulse, 1.0F);
    }

    public static void pan(float leftImpulse, float forwardImpulse, float speedMultiplier) {
        if (leftImpulse == 0.0F && forwardImpulse == 0.0F) {
            return;
        }

        RtsTargetingState.stopFollowing();

        Vec3 forward = Vec3.directionFromRotation(0.0F, targetYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        double panDistance = usesPerspectiveRig() && !absolutePlacementOrbitActive
                ? absolutePanDistance()
                : targetDistance;
        double speed = Mth.clamp(panDistance / 48.0D, 0.35D, 2.5D)
                * Mth.clamp(speedMultiplier, 0.25F, 3.0F);
        Vec3 delta = forward.scale(forwardImpulse * speed).add(left.scale(leftImpulse * speed));
        targetCenterX += delta.x;
        targetCenterZ += delta.z;
        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            targetAbsoluteX += delta.x;
            targetAbsoluteZ += delta.z;
        }
    }

    public static void panFromScreenDrag(double deltaX, double deltaY) {
        if (!active || deltaX == 0.0D && deltaY == 0.0D) {
            return;
        }

        RtsTargetingState.stopFollowing();

        Vec3 forward = Vec3.directionFromRotation(0.0F, targetYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        double panDistance = usesPerspectiveRig() && !absolutePlacementOrbitActive
                ? absolutePanDistance()
                : targetDistance;
        double worldUnitsPerPixel = Mth.clamp(panDistance / 700.0D, 0.02D, 0.18D);
        Vec3 delta = forward.scale(deltaY * worldUnitsPerPixel).add(left.scale(deltaX * worldUnitsPerPixel));
        targetCenterX += delta.x;
        targetCenterZ += delta.z;
        if (usesPerspectiveRig() && !absolutePlacementOrbitActive) {
            targetAbsoluteX += delta.x;
            targetAbsoluteZ += delta.z;
        }
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

        RtsTargetingState.stopFollowing();

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

        RtsTargetingState.stopFollowing();

        if (isTrueIsometric()) {
            rotateIsometricFromScreenDrag(deltaX);
            return;
        }

        targetYaw = Mth.wrapDegrees((float) (targetYaw + deltaX * DRAG_ROTATE_DEGREES_PER_PIXEL));
    }

    public static void orbitPerspectiveFromScreenDrag(double deltaX, boolean invertHorizontal) {
        if (!active
                || !usesPerspectiveRig()
                || absolutePlacementOrbitActive
                || deltaX == 0.0D) {
            return;
        }

        RtsTargetingState.stopFollowing();
        orbitAbsoluteCameraHorizontally(invertHorizontal ? -deltaX : deltaX);
    }

    public static void rotateIsometricFromScreenDrag(double deltaX) {
        if (!active || deltaX == 0.0D) {
            return;
        }

        RtsTargetingState.stopFollowing();

        isometricDragAccumulator += deltaX;
        while (Math.abs(isometricDragAccumulator) >= ISOMETRIC_ROTATE_DRAG_PIXELS) {
            float direction = isometricDragAccumulator > 0.0D ? 1.0F : -1.0F;
            targetYaw = Mth.wrapDegrees(targetYaw + direction * 90.0F);
            renderYaw = targetYaw;
            isometricDragAccumulator -= direction * ISOMETRIC_ROTATE_DRAG_PIXELS;
        }
    }

    public static void updateTerrainHeight(ClientLevel level, boolean stabilized) {
        if (!usesTerrainFollowing()) {
            return;
        }

        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(targetCenterX), Mth.floor(targetCenterZ));
        double terrainY = height + 1.0D;
        if (!terrainHeightOffsetInitialized) {
            terrainHeightOffset = targetCenterY - terrainY;
            terrainHeightOffsetInitialized = true;
            return;
        }

        double desiredCenterY = terrainY + terrainHeightOffset;
        double heightDelta = stabilized
                ? (desiredCenterY - targetCenterY) * 0.035D
                : desiredCenterY - targetCenterY;
        shiftPerspectiveRigHeight(heightDelta);
    }

    public static Matrix4f getIsometricProjection(Minecraft minecraft) {
        float verticalSpan = getIsometricVerticalSpan();
        float horizontalSpan = getIsometricHorizontalSpan(minecraft);
        return new Matrix4f().setOrtho(
                -horizontalSpan / 2.0F,
                horizontalSpan / 2.0F,
                -verticalSpan / 2.0F,
                verticalSpan / 2.0F,
                -3000.0F,
                3000.0F
        );
    }

    public static float getIsometricVerticalSpan() {
        return Math.max(12.0F, renderDistance * 1.5F);
    }

    public static float getIsometricHorizontalSpan(Minecraft minecraft) {
        float aspectRatio = (float) minecraft.getWindow().getScreenWidth()
                / Math.max(1, minecraft.getWindow().getScreenHeight());
        return getIsometricVerticalSpan() * aspectRatio;
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

    private static void focusAbsoluteCameraOn(Vec3 center) {
        double distance = absolutePanDistance();
        Vec3 lookDirection = Vec3.directionFromRotation(targetPitch, targetYaw).normalize();
        targetAbsoluteX = center.x - lookDirection.x * distance;
        targetAbsoluteY = center.y - lookDirection.y * distance;
        targetAbsoluteZ = center.z - lookDirection.z * distance;
    }

    private static void orbitAbsoluteCameraHorizontally(double deltaX) {
        double offsetX = targetAbsoluteX - targetCenterX;
        double offsetZ = targetAbsoluteZ - targetCenterZ;
        double horizontalDistance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (horizontalDistance <= 1.0E-6D) {
            return;
        }

        Vec3 lookOffset = new Vec3(-offsetX, targetCenterY - targetAbsoluteY, -offsetZ);
        float orbitYaw = Mth.wrapDegrees(
                yawFromDirection(lookOffset) + (float) (deltaX * DRAG_ROTATE_DEGREES_PER_PIXEL)
        );
        Vec3 horizontalLookDirection = Vec3.directionFromRotation(0.0F, orbitYaw);
        targetAbsoluteX = targetCenterX - horizontalLookDirection.x * horizontalDistance;
        targetAbsoluteZ = targetCenterZ - horizontalLookDirection.z * horizontalDistance;
        updateAbsoluteTargetLookAt();
    }

    private static double absolutePanDistance() {
        double deltaX = targetAbsoluteX - targetCenterX;
        double deltaY = targetAbsoluteY - targetCenterY;
        double deltaZ = targetAbsoluteZ - targetCenterZ;
        return Math.max(MIN_DISTANCE, Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
    }

    private static void shiftPerspectiveRigHeight(double deltaY) {
        if (Math.abs(deltaY) <= 1.0E-9D) {
            return;
        }
        targetCenterY += deltaY;
        targetAbsoluteY += deltaY;
    }

    private static void resetAbsolutePositionFromOrbit() {
        Vec3 lookDirection = Vec3.directionFromRotation(targetPitch, targetYaw).normalize();
        targetAbsoluteX = targetCenterX - lookDirection.x * targetDistance;
        targetAbsoluteY = targetCenterY - lookDirection.y * targetDistance;
        targetAbsoluteZ = targetCenterZ - lookDirection.z * targetDistance;
        renderAbsoluteX = targetAbsoluteX;
        renderAbsoluteY = targetAbsoluteY;
        renderAbsoluteZ = targetAbsoluteZ;
        updateAbsoluteTargetLookAt();
        updateAbsoluteRenderLookAt();
    }

    private static void captureAbsolutePlacementRestore() {
        absoluteRestoreX = renderAbsoluteX;
        absoluteRestoreY = renderAbsoluteY;
        absoluteRestoreZ = renderAbsoluteZ;
        absoluteRestoreCenterX = renderCenterX;
        absoluteRestoreCenterY = renderCenterY;
        absoluteRestoreCenterZ = renderCenterZ;
        absoluteRestoreYaw = renderYaw;
        absoluteRestorePitch = renderPitch;
        absoluteRestoreDistance = renderDistance;
        absolutePlacementRestoreAvailable = true;
    }

    private static void restoreAbsoluteAfterPlacement() {
        if (absolutePlacementRestoreAvailable) {
            targetAbsoluteX = absoluteRestoreX;
            targetAbsoluteY = absoluteRestoreY;
            targetAbsoluteZ = absoluteRestoreZ;
            renderAbsoluteX = targetAbsoluteX;
            renderAbsoluteY = targetAbsoluteY;
            renderAbsoluteZ = targetAbsoluteZ;
            targetCenterX = absoluteRestoreCenterX;
            targetCenterY = absoluteRestoreCenterY;
            targetCenterZ = absoluteRestoreCenterZ;
            renderCenterX = targetCenterX;
            renderCenterY = targetCenterY;
            renderCenterZ = targetCenterZ;
            targetYaw = absoluteRestoreYaw;
            targetPitch = absoluteRestorePitch;
            renderYaw = targetYaw;
            renderPitch = targetPitch;
            targetDistance = absoluteRestoreDistance;
            renderDistance = targetDistance;
        } else if (absolutePlacementOrbitActive) {
            Vec3 orbitPosition = getCenter().subtract(
                    Vec3.directionFromRotation(renderPitch, renderYaw).normalize().scale(renderDistance)
            );
            targetAbsoluteX = orbitPosition.x;
            targetAbsoluteY = orbitPosition.y;
            targetAbsoluteZ = orbitPosition.z;
            renderAbsoluteX = targetAbsoluteX;
            renderAbsoluteY = targetAbsoluteY;
            renderAbsoluteZ = targetAbsoluteZ;
        }

        clearAbsolutePlacementRestore();
        updateAbsoluteTargetLookAt();
        updateAbsoluteRenderLookAt();
        lastRenderNanos = 0L;
    }

    private static void clearAbsolutePlacementRestore() {
        absolutePlacementOrbitActive = false;
        absolutePlacementRestoreAvailable = false;
    }

    private static float yawFromDirection(Vec3 direction) {
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
    }

    private static float pitchFromDirection(Vec3 direction) {
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) Math.toDegrees(Math.atan2(-direction.y, horizontalLength));
    }

    private static void updateAbsoluteTargetLookAt() {
        Vec3 lookOffset = new Vec3(
                targetCenterX - targetAbsoluteX,
                targetCenterY - targetAbsoluteY,
                targetCenterZ - targetAbsoluteZ
        );
        if (lookOffset.lengthSqr() <= 1.0E-12D) {
            return;
        }
        targetYaw = yawFromDirection(lookOffset);
        targetPitch = pitchForMode();
    }

    private static void updateAbsoluteRenderLookAt() {
        Vec3 lookOffset = new Vec3(
                renderCenterX - renderAbsoluteX,
                renderCenterY - renderAbsoluteY,
                renderCenterZ - renderAbsoluteZ
        );
        if (lookOffset.lengthSqr() <= 1.0E-12D) {
            return;
        }
        renderYaw = yawFromDirection(lookOffset);
        renderPitch = pitchForMode();
    }

    private static void resetRenderState() {
        renderCenterX = targetCenterX;
        renderCenterY = targetCenterY;
        renderCenterZ = targetCenterZ;
        renderYaw = targetYaw;
        renderPitch = targetPitch;
        renderDistance = targetDistance;
        lastRenderNanos = 0L;
        isometricDragAccumulator = 0.0D;
    }

    private static void enterFreecam() {
        if (!active || RtsBuildDrawer.isPreviewActive()) {
            return;
        }

        Vec3 cameraPosition = getCameraPosition();
        Vec3 center = getCenter();
        float freecamYaw = renderYaw;
        float freecamPitch = renderPitch;
        double distance = Math.max(MIN_DISTANCE, cameraPosition.distanceTo(center));

        if (!RtsFreecamIntegration.enableAt(cameraPosition, freecamYaw, freecamPitch)) {
            RTColony.LOGGER.warn("Freecam refused activation; keeping RTS camera mode {}", mode);
            return;
        }

        targetCenterX = center.x;
        targetCenterY = center.y;
        targetCenterZ = center.z;
        targetYaw = freecamYaw;
        targetPitch = DEFAULT_PITCH;
        targetDistance = (float) distance;
        Vec3 destinationDirection = Vec3.directionFromRotation(targetPitch, targetYaw).normalize();
        targetAbsoluteX = targetCenterX - destinationDirection.x * distance;
        targetAbsoluteY = targetCenterY - destinationDirection.y * distance;
        targetAbsoluteZ = targetCenterZ - destinationDirection.z * distance;

        mode = RtsCameraMode.FREECAM;
        freecamReturnActive = false;
        clearAbsolutePlacementRestore();
        terrainHeightOffsetInitialized = false;
        isometricDragAccumulator = 0.0D;
        RtsBuildDrawer.close();
        RtsTargetingState.clear();
        RTColony.LOGGER.info("RTS camera mode: freecam");
    }

    private static void beginFreecamReturn() {
        beginFreecamReturn(true);
    }

    private static void beginFreecamReturn(boolean disableFreecam) {
        if (!isFreecam()) {
            return;
        }

        RtsFreecamIntegration.VisualPose source = RtsFreecamIntegration.captureVisualPose();
        if (disableFreecam) {
            RtsFreecamIntegration.disable();
        }

        mode = RtsCameraMode.PERSPECTIVE;
        freecamReturnSourceX = source.position().x;
        freecamReturnSourceY = source.position().y;
        freecamReturnSourceZ = source.position().z;
        freecamReturnSourceYaw = source.yaw();
        freecamReturnSourcePitch = source.pitch();
        Vec3 sourceDirection = Vec3.directionFromRotation(source.pitch(), source.yaw()).normalize();
        freecamReturnSourceCenterX = source.position().x + sourceDirection.x * targetDistance;
        freecamReturnSourceCenterY = source.position().y + sourceDirection.y * targetDistance;
        freecamReturnSourceCenterZ = source.position().z + sourceDirection.z * targetDistance;

        renderAbsoluteX = freecamReturnSourceX;
        renderAbsoluteY = freecamReturnSourceY;
        renderAbsoluteZ = freecamReturnSourceZ;
        renderCenterX = freecamReturnSourceCenterX;
        renderCenterY = freecamReturnSourceCenterY;
        renderCenterZ = freecamReturnSourceCenterZ;
        renderYaw = freecamReturnSourceYaw;
        renderPitch = freecamReturnSourcePitch;
        renderDistance = targetDistance;
        targetPitch = DEFAULT_PITCH;
        freecamReturnStartNanos = System.nanoTime();
        freecamReturnActive = true;
        lastRenderNanos = 0L;
        clearAbsolutePlacementRestore();
        terrainHeightOffsetInitialized = false;
        RTColony.LOGGER.info("RTS camera mode: returning from freecam to perspective");
    }

    private static void advanceFreecamReturn(long now) {
        double progress = Mth.clamp(
                (now - freecamReturnStartNanos) / (double) FREECAM_RETURN_DURATION_NANOS,
                0.0D,
                1.0D
        );
        double easedProgress = progress * progress * (3.0D - 2.0D * progress);
        float easedRotationProgress = (float) easedProgress;

        renderAbsoluteX = Mth.lerp(easedProgress, freecamReturnSourceX, targetAbsoluteX);
        renderAbsoluteY = Mth.lerp(easedProgress, freecamReturnSourceY, targetAbsoluteY);
        renderAbsoluteZ = Mth.lerp(easedProgress, freecamReturnSourceZ, targetAbsoluteZ);
        renderCenterX = Mth.lerp(easedProgress, freecamReturnSourceCenterX, targetCenterX);
        renderCenterY = Mth.lerp(easedProgress, freecamReturnSourceCenterY, targetCenterY);
        renderCenterZ = Mth.lerp(easedProgress, freecamReturnSourceCenterZ, targetCenterZ);
        renderYaw = Mth.rotLerp(easedRotationProgress, freecamReturnSourceYaw, targetYaw);
        renderPitch = Mth.lerp(easedRotationProgress, freecamReturnSourcePitch, targetPitch);
        renderDistance = targetDistance;

        if (progress >= 1.0D) {
            renderAbsoluteX = targetAbsoluteX;
            renderAbsoluteY = targetAbsoluteY;
            renderAbsoluteZ = targetAbsoluteZ;
            renderCenterX = targetCenterX;
            renderCenterY = targetCenterY;
            renderCenterZ = targetCenterZ;
            renderYaw = targetYaw;
            renderPitch = targetPitch;
            freecamReturnActive = false;
            lastRenderNanos = now;
            RTColony.LOGGER.info("RTS camera mode: perspective");
        }
    }

    private static float pitchForMode() {
        return switch (mode) {
            case PERSPECTIVE -> DEFAULT_PITCH;
            case FIXED_ANGLE -> FIXED_ANGLE_PITCH;
            case TRUE_ISOMETRIC -> ISOMETRIC_PITCH;
            case FREECAM -> renderPitch;
        };
    }

    private static float snapToIsometricYaw(float yaw) {
        return Mth.wrapDegrees(45.0F + Math.round((yaw - 45.0F) / 90.0F) * 90.0F);
    }
}
