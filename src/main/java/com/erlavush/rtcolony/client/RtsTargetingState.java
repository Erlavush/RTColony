package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.mixin.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;

public final class RtsTargetingState {
    private static final double PICK_DISTANCE = 160.0D;

    private static HitResult hoverHit;
    private static TargetSnapshot hoveredTarget;
    private static TargetSnapshot selectedTarget;
    private static int followedEntityId = -1;

    private RtsTargetingState() {
    }

    public static void clear() {
        hoverHit = null;
        hoveredTarget = null;
        selectedTarget = null;
        followedEntityId = -1;
        RtsCutawayState.clear();
        RtsMineColoniesIntegration.clearCache();
    }

    public static void updateHover(Minecraft minecraft) {
        if (!RtsModeState.isEnabled()
                || !RtsCameraState.isActive()
                || minecraft.level == null
                || minecraft.player == null
                || minecraft.screen != null) {
            hoverHit = null;
            hoveredTarget = null;
            return;
        }

        hoverHit = raycastFromCursor(minecraft);
        hoveredTarget = TargetSnapshot.from(minecraft.level, hoverHit);
        minecraft.hitResult = hoverHit;
        minecraft.crosshairPickEntity = hoverHit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    public static void selectHovered() {
        if (followedEntityId >= 0
                && (hoveredTarget == null
                || hoveredTarget.kind() != TargetKind.ENTITY
                || hoveredTarget.entity() == null
                || hoveredTarget.entity().getId() != followedEntityId)) {
            stopFollowing();
        }
        selectedTarget = hoveredTarget;
    }

    static boolean toggleFollowSelected() {
        if (selectedTarget == null
                || selectedTarget.kind() != TargetKind.ENTITY
                || selectedTarget.entity() == null
                || !selectedTarget.entity().isAlive()) {
            return false;
        }

        int entityId = selectedTarget.entity().getId();
        if (followedEntityId == entityId) {
            followedEntityId = -1;
        } else {
            followedEntityId = entityId;
        }
        return true;
    }

    static boolean tickFollow(Minecraft minecraft) {
        Entity entity = getFollowedEntity(minecraft);
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            followedEntityId = -1;
            return false;
        }

        RtsCameraState.focusOn(entity.getBoundingBox().getCenter());
        return true;
    }

    public static boolean stopFollowing() {
        if (followedEntityId < 0) {
            return false;
        }
        followedEntityId = -1;
        return true;
    }

    static boolean isFollowingSelected() {
        return selectedTarget != null
                && selectedTarget.entity() != null
                && selectedTarget.entity().getId() == followedEntityId;
    }

    static Entity getFollowedEntity(Minecraft minecraft) {
        if (followedEntityId < 0 || minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(followedEntityId);
    }

    public static TargetSnapshot getHoveredTarget() {
        return hoveredTarget;
    }

    public static HitResult getHoverHit() {
        return hoverHit;
    }

    public static TargetSnapshot getSelectedTarget() {
        return selectedTarget;
    }

    private static HitResult raycastFromCursor(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = RtsCameraState.isTrueIsometric()
                ? getIsometricCursorRayOrigin(minecraft, camera)
                : camera.isInitialized() ? camera.getPosition() : RtsCameraState.getCameraPosition();
        Vec3 direction = RtsCameraState.isTrueIsometric()
                ? getIsometricCursorRayDirection(camera)
                : getCursorRayDirection(minecraft, camera);
        Vec3 end = origin.add(direction.scale(PICK_DISTANCE));
        BlockHitResult blockHit = minecraft.level.clip(new ClipContext(
                origin,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));

        double hitDistanceSqr = PICK_DISTANCE * PICK_DISTANCE;
        Vec3 clippedEnd = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            hitDistanceSqr = origin.distanceToSqr(blockHit.getLocation());
            clippedEnd = blockHit.getLocation();
        }

        AABB searchBox = new AABB(origin, clippedEnd).inflate(2.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                origin,
                clippedEnd,
                searchBox,
                entity -> entity != minecraft.player && !entity.isSpectator() && entity.isPickable(),
                hitDistanceSqr
        );

        if (entityHit != null) {
            return entityHit;
        }

        return blockHit;
    }

    private static Vec3 getIsometricCursorRayOrigin(Minecraft minecraft, Camera camera) {
        if (!camera.isInitialized()) {
            return RtsCameraState.getCameraPosition();
        }

        double mouseX = Mth.clamp(minecraft.mouseHandler.xpos(), 0.0D, minecraft.getWindow().getScreenWidth());
        double mouseY = Mth.clamp(minecraft.mouseHandler.ypos(), 0.0D, minecraft.getWindow().getScreenHeight());
        double width = Math.max(1, minecraft.getWindow().getScreenWidth());
        double height = Math.max(1, minecraft.getWindow().getScreenHeight());
        float normalizedX = (float) (mouseX / width * 2.0D - 1.0D);
        float normalizedY = (float) (1.0D - mouseY / height * 2.0D);

        // In an orthographic view every cursor ray is parallel. Its origin must
        // move across the camera plane, rather than always starting at the
        // camera itself as it does with a perspective projection.
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double horizontalOffset = normalizedX * RtsCameraState.getIsometricHorizontalSpan(minecraft) / 2.0D;
        double verticalOffset = normalizedY * RtsCameraState.getIsometricVerticalSpan() / 2.0D;
        return camera.getPosition()
                // Camera exposes its left axis, so screen-right is its inverse.
                .add(-left.x() * horizontalOffset + up.x() * verticalOffset,
                        -left.y() * horizontalOffset + up.y() * verticalOffset,
                        -left.z() * horizontalOffset + up.z() * verticalOffset);
    }

    private static Vec3 getIsometricCursorRayDirection(Camera camera) {
        if (!camera.isInitialized()) {
            return Vec3.directionFromRotation(RtsCameraState.getPitch(), RtsCameraState.getYaw()).normalize();
        }

        Vector3f direction = camera.getLookVector();
        return new Vec3(direction.x(), direction.y(), direction.z());
    }

    private static Vec3 getCursorRayDirection(Minecraft minecraft, Camera camera) {
        double mouseX = Mth.clamp(minecraft.mouseHandler.xpos(), 0.0D, minecraft.getWindow().getScreenWidth());
        double mouseY = Mth.clamp(minecraft.mouseHandler.ypos(), 0.0D, minecraft.getWindow().getScreenHeight());
        double width = Math.max(1, minecraft.getWindow().getScreenWidth());
        double height = Math.max(1, minecraft.getWindow().getScreenHeight());
        double normalizedX = mouseX / width * 2.0D - 1.0D;
        double normalizedY = 1.0D - mouseY / height * 2.0D;

        if (camera.isInitialized()) {
            double fov = ((GameRendererAccessor) minecraft.gameRenderer).rtcolony$getFov(camera, camera.getPartialTickTime(), true);
            Matrix4f inverseProjection = new Matrix4f(minecraft.gameRenderer.getProjectionMatrix(fov)).invert();
            Vector4f cameraSpace = new Vector4f((float) normalizedX, (float) normalizedY, 1.0F, 1.0F).mul(inverseProjection);
            if (Math.abs(cameraSpace.w()) > 1.0E-6F) {
                cameraSpace.div(cameraSpace.w());
            }

            Vector3f worldSpace = new Vector3f(cameraSpace.x(), cameraSpace.y(), cameraSpace.z()).normalize();
            worldSpace.rotate(camera.rotation()).normalize();
            return new Vec3(worldSpace.x(), worldSpace.y(), worldSpace.z()).normalize();
        }

        return Vec3.directionFromRotation(RtsCameraState.getPitch(), RtsCameraState.getYaw()).normalize();
    }

    public record TargetSnapshot(
            TargetKind kind,
            BlockPos blockPos,
            Entity entity,
            Component title,
            Component detail,
            AABB selectionBounds
    ) {
        private static TargetSnapshot from(ClientLevel level, HitResult hitResult) {
            if (hitResult instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                return new TargetSnapshot(
                        TargetKind.ENTITY,
                        null,
                        entity,
                        entity.getDisplayName(),
                        Component.literal(entityId + " #" + entity.getId()),
                        null
                );
            }

            if (hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = blockHit.getBlockPos();
                Optional<RtsMineColoniesIntegration.BuildingTarget> building =
                        RtsMineColoniesIntegration.findBuildingAt(level, pos);
                if (building.isPresent()) {
                    RtsMineColoniesIntegration.BuildingTarget target = building.get();
                    return new TargetSnapshot(
                            TargetKind.BUILDING,
                            target.anchor(),
                            null,
                            Component.literal(target.info().title()),
                            Component.literal("MineColonies building @ " + target.anchor().toShortString()),
                            target.bounds()
                    );
                }

                BlockState state = level.getBlockState(pos);
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                return new TargetSnapshot(
                        TargetKind.BLOCK,
                        pos,
                        null,
                        state.getBlock().getName(),
                        Component.literal(blockId + " @ " + pos.toShortString()),
                        null
                );
            }

            return null;
        }

        public AABB outlineBox() {
            if (this.kind == TargetKind.ENTITY && this.entity != null) {
                return this.entity.getBoundingBox().inflate(0.08D);
            }
            if (this.kind == TargetKind.BLOCK && this.blockPos != null) {
                return new AABB(this.blockPos).inflate(0.002D);
            }
            if (this.kind == TargetKind.BUILDING && this.selectionBounds != null) {
                return this.selectionBounds.inflate(0.01D);
            }
            return null;
        }

        public boolean sameTarget(TargetSnapshot other) {
            if (other == null || this.kind != other.kind) {
                return false;
            }
            if (this.kind == TargetKind.ENTITY) {
                return this.entity != null && other.entity != null && this.entity.getId() == other.entity.getId();
            }
            return this.blockPos != null && this.blockPos.equals(other.blockPos);
        }
    }

    public enum TargetKind {
        BLOCK,
        BUILDING,
        ENTITY
    }
}
