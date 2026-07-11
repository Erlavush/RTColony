package com.erlavush.rtcolony.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Determines whether the selected/followed entity is genuinely hidden by
 * foreground terrain and exposes a stable snapshot for the terrain cutaway
 * shader.
 *
 * <p>This class performs all gameplay/view-state decisions. The shader is only
 * responsible for drawing the already-approved cutaway.</p>
 */
public final class RtsCutawayState {
    private static final int REQUIRED_BLOCKED_SAMPLES = 3;
    private static final int ACTIVATE_AFTER_TICKS = 2;
    private static final int DEACTIVATE_AFTER_TICKS = 3;

    private static final float OPEN_STEP_PER_TICK = 0.28F;
    private static final float CLOSE_STEP_PER_TICK = 0.38F;

    private static final double SAMPLE_END_MARGIN = 0.20D;
    private static final double PASSTHROUGH_STEP = 0.08D;
    private static final int MAX_PASSTHROUGH_HITS = 10;

    private static final double SUPPORT_FACE_HEIGHT_MARGIN = 0.18D;
    private static final double SUPPORT_HIT_END_DISTANCE = 0.85D;

    private static int trackedEntityId = -1;
    private static AABB trackedBounds;
    private static Vec3 trackedCenter;

    private static int obstructedTicks;
    private static int visibleTicks;
    private static boolean requestedOpen;
    private static float openAmount;
    private static Snapshot snapshot;

    private RtsCutawayState() {
    }

    /**
     * Update once per client tick after the RTS camera and follow target have
     * been updated.
     */
    public static void tick(Minecraft minecraft) {
        Entity target = resolveEligibleTarget(minecraft);

        if (target == null) {
            requestClose();
            advanceAnimation();
            publishSnapshot();
            return;
        }

        if (target.getId() != trackedEntityId) {
            beginTracking(target);
        } else {
            updateTrackedGeometry(target);
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            requestClose();
            advanceAnimation();
            publishSnapshot();
            return;
        }

        boolean obstructed = isTargetObstructed(
                minecraft.level,
                minecraft.player,
                camera,
                target.getBoundingBox()
        );

        if (obstructed) {
            visibleTicks = 0;
            obstructedTicks++;

            if (obstructedTicks >= ACTIVATE_AFTER_TICKS) {
                requestedOpen = true;
            }
        } else {
            obstructedTicks = 0;
            visibleTicks++;

            if (visibleTicks >= DEACTIVATE_AFTER_TICKS) {
                requestedOpen = false;
            }
        }

        advanceAnimation();
        publishSnapshot();
    }

    public static boolean isRendering() {
        return snapshot != null && snapshot.openAmount() > 0.001F;
    }

    public static Snapshot getSnapshot() {
        return snapshot;
    }

    public static void clear() {
        trackedEntityId = -1;
        trackedBounds = null;
        trackedCenter = null;
        obstructedTicks = 0;
        visibleTicks = 0;
        requestedOpen = false;
        openAmount = 0.0F;
        snapshot = null;
    }

    private static Entity resolveEligibleTarget(Minecraft minecraft) {
        if (minecraft == null
                || !RtsModeState.isEnabled()
                || !RtsCameraState.isActive()
                || minecraft.level == null
                || minecraft.player == null
                || minecraft.screen != null
                || RtsBuildDrawer.isPreviewActive()) {
            return null;
        }

        Entity followed = RtsTargetingState.getFollowedEntity(minecraft);
        if (isUsableEntity(followed)) {
            return followed;
        }

        RtsTargetingState.TargetSnapshot selected = RtsTargetingState.getSelectedTarget();
        if (selected != null
                && selected.kind() == RtsTargetingState.TargetKind.ENTITY
                && isUsableEntity(selected.entity())) {
            return selected.entity();
        }

        return null;
    }

    private static boolean isUsableEntity(Entity entity) {
        return entity != null
                && entity.isAlive()
                && !entity.isRemoved();
    }

    private static void beginTracking(Entity target) {
        trackedEntityId = target.getId();
        trackedBounds = target.getBoundingBox();
        trackedCenter = trackedBounds.getCenter();

        obstructedTicks = 0;
        visibleTicks = 0;
        requestedOpen = false;

        // Never leave the old target's cutaway on-screen while switching.
        openAmount = 0.0F;
        snapshot = null;
    }

    private static void updateTrackedGeometry(Entity target) {
        trackedBounds = target.getBoundingBox();
        trackedCenter = trackedBounds.getCenter();
    }

    private static void requestClose() {
        obstructedTicks = 0;
        visibleTicks = DEACTIVATE_AFTER_TICKS;
        requestedOpen = false;
    }

    private static void advanceAnimation() {
        float targetAmount = requestedOpen ? 1.0F : 0.0F;
        float step = requestedOpen ? OPEN_STEP_PER_TICK : CLOSE_STEP_PER_TICK;

        if (openAmount < targetAmount) {
            openAmount = Math.min(targetAmount, openAmount + step);
        } else if (openAmount > targetAmount) {
            openAmount = Math.max(targetAmount, openAmount - step);
        }

        if (!requestedOpen && openAmount <= 0.001F) {
            openAmount = 0.0F;

            if (visibleTicks >= DEACTIVATE_AFTER_TICKS) {
                trackedEntityId = -1;
                trackedBounds = null;
                trackedCenter = null;
            }
        }
    }

    private static void publishSnapshot() {
        if (trackedEntityId < 0
                || trackedBounds == null
                || trackedCenter == null
                || openAmount <= 0.001F) {
            snapshot = null;
            return;
        }

        snapshot = new Snapshot(
                trackedEntityId,
                trackedBounds,
                trackedCenter,
                openAmount
        );
    }

    private static boolean isTargetObstructed(
            ClientLevel level,
            LocalPlayer player,
            Camera camera,
            AABB targetBounds
    ) {
        if (level == null || player == null || targetBounds == null) {
            return false;
        }

        List<Vec3> samples = createTargetSamples(camera, targetBounds);
        int blockedSamples = 0;
        boolean centerBlocked = false;

        for (int index = 0; index < samples.size(); index++) {
            Vec3 sample = samples.get(index);
            Vec3 rayStart = rayStartForSample(camera, sample);

            if (isSampleBlocked(
                    level,
                    player,
                    rayStart,
                    sample,
                    targetBounds
            )) {
                blockedSamples++;

                if (index == 0) {
                    centerBlocked = true;
                }
            }
        }

        // The body center must be obstructed, and enough surrounding samples
        // must also be blocked to avoid activating for tiny edge overlaps.
        return centerBlocked && blockedSamples >= REQUIRED_BLOCKED_SAMPLES;
    }

    private static List<Vec3> createTargetSamples(Camera camera, AABB bounds) {
        Vec3 center = bounds.getCenter();

        Vector3f lookVector = camera.getLookVector();
        Vec3 look = new Vec3(
                lookVector.x(),
                lookVector.y(),
                lookVector.z()
        ).normalize();

        Vec3 screenRight = new Vec3(-look.z, 0.0D, look.x);
        if (screenRight.lengthSqr() < 1.0E-8D) {
            screenRight = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            screenRight = screenRight.normalize();
        }

        double height = Math.max(0.1D, bounds.getYsize());
        double horizontalSize = Math.max(bounds.getXsize(), bounds.getZsize());
        double horizontalOffset = Mth.clamp(horizontalSize * 0.38D, 0.18D, 0.55D);

        double lowerY = bounds.minY + height * 0.28D;
        double upperY = bounds.minY + height * 0.72D;
        double headY = bounds.minY + height * 0.88D;

        Vec3 rightOffset = screenRight.scale(horizontalOffset);

        return List.of(
                center,
                new Vec3(center.x, headY, center.z),
                new Vec3(center.x, lowerY, center.z),
                new Vec3(center.x, upperY, center.z).add(rightOffset),
                new Vec3(center.x, upperY, center.z).subtract(rightOffset),
                center.add(rightOffset),
                center.subtract(rightOffset)
        );
    }

    private static Vec3 rayStartForSample(Camera camera, Vec3 sample) {
        Vec3 cameraPosition = camera.getPosition();

        if (!RtsCameraState.isTrueIsometric()) {
            return cameraPosition;
        }

        // Orthographic camera rays are parallel. Find the point on the camera
        // plane corresponding to this sample instead of incorrectly firing
        // every sample from one perspective-camera origin.
        Vector3f lookVector = camera.getLookVector();
        Vec3 look = new Vec3(
                lookVector.x(),
                lookVector.y(),
                lookVector.z()
        ).normalize();

        double depthFromCameraPlane = sample.subtract(cameraPosition).dot(look);
        if (depthFromCameraPlane <= 0.0D) {
            return cameraPosition;
        }

        return sample.subtract(look.scale(depthFromCameraPlane));
    }

    private static boolean isSampleBlocked(
            ClientLevel level,
            LocalPlayer player,
            Vec3 start,
            Vec3 end,
            AABB targetBounds
    ) {
        Vec3 delta = end.subtract(start);
        double totalDistance = delta.length();

        if (totalDistance <= SAMPLE_END_MARGIN) {
            return false;
        }

        Vec3 direction = delta.scale(1.0D / totalDistance);
        Vec3 cursor = start;

        for (int pass = 0; pass < MAX_PASSTHROUGH_HITS; pass++) {
            BlockHitResult hit = level.clip(new ClipContext(
                    cursor,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (hit.getType() != HitResult.Type.BLOCK) {
                return false;
            }

            Vec3 hitLocation = hit.getLocation();
            double distanceFromStart = start.distanceTo(hitLocation);

            // A hit almost at the entity sample is usually the floor or block
            // touching the entity, not a foreground obstruction.
            if (distanceFromStart >= totalDistance - SAMPLE_END_MARGIN) {
                return false;
            }

            BlockPos blockPos = hit.getBlockPos();
            BlockState blockState = level.getBlockState(blockPos);

            if (isTargetSupportSurface(
                    hit,
                    hitLocation,
                    end,
                    targetBounds
            )) {
                return false;
            }

            if (isVisualBlocker(level, blockPos, blockState)) {
                return true;
            }

            Vec3 nextCursor = hitLocation.add(direction.scale(PASSTHROUGH_STEP));
            if (nextCursor.distanceToSqr(cursor) < 1.0E-8D) {
                return false;
            }

            cursor = nextCursor;

            if (start.distanceTo(cursor) >= totalDistance - SAMPLE_END_MARGIN) {
                return false;
            }
        }

        return false;
    }

    private static boolean isTargetSupportSurface(
            BlockHitResult hit,
            Vec3 hitLocation,
            Vec3 targetSample,
            AABB targetBounds
    ) {
        if (hit.getDirection() != Direction.UP) {
            return false;
        }

        double protectedFloorY =
                targetBounds.minY + SUPPORT_FACE_HEIGHT_MARGIN;

        if (hitLocation.y > protectedFloorY) {
            return false;
        }

        return hitLocation.distanceTo(targetSample)
                <= SUPPORT_HIT_END_DISTANCE;
    }

    private static boolean isVisualBlocker(
            ClientLevel level,
            BlockPos blockPos,
            BlockState blockState
    ) {
        if (blockState.isAir()) {
            return false;
        }

        // Leaves commonly obstruct the RTS camera even though they are not
        // treated like full opaque cubes by every vanilla shape method.
        if (blockState.is(BlockTags.LEAVES)) {
            return true;
        }

        // Glass and similarly non-occluding blocks should not trigger a
        // cutaway merely because they have collision.
        return blockState.canOcclude()
                || !blockState.getOcclusionShape(level, blockPos).isEmpty();
    }

    public record Snapshot(
            int entityId,
            AABB targetBounds,
            Vec3 targetCenter,
            float openAmount
    ) {
    }
}
