package com.erlavush.rtcolony.client;

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

public final class RtsTargetingState {
    private static final double PICK_DISTANCE = 160.0D;

    private static HitResult hoverHit;
    private static TargetSnapshot hoveredTarget;
    private static TargetSnapshot selectedTarget;

    private RtsTargetingState() {
    }

    public static void clear() {
        hoverHit = null;
        hoveredTarget = null;
        selectedTarget = null;
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
        selectedTarget = hoveredTarget;
    }

    public static TargetSnapshot getHoveredTarget() {
        return hoveredTarget;
    }

    public static TargetSnapshot getSelectedTarget() {
        return selectedTarget;
    }

    private static HitResult raycastFromCursor(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.isInitialized() ? camera.getPosition() : RtsCameraState.getCameraPosition();
        Vec3 direction = getCursorRayDirection(minecraft, camera);
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

    private static Vec3 getCursorRayDirection(Minecraft minecraft, Camera camera) {
        double mouseX = Mth.clamp(minecraft.mouseHandler.xpos(), 0.0D, minecraft.getWindow().getScreenWidth());
        double mouseY = Mth.clamp(minecraft.mouseHandler.ypos(), 0.0D, minecraft.getWindow().getScreenHeight());
        double width = Math.max(1, minecraft.getWindow().getScreenWidth());
        double height = Math.max(1, minecraft.getWindow().getScreenHeight());
        double normalizedX = mouseX / width * 2.0D - 1.0D;
        double normalizedY = 1.0D - mouseY / height * 2.0D;

        if (camera.isInitialized()) {
            return camera.getNearPlane().getPointOnPlane((float) normalizedX, (float) normalizedY).normalize();
        }

        return Vec3.directionFromRotation(RtsCameraState.getPitch(), RtsCameraState.getYaw()).normalize();
    }

    public record TargetSnapshot(
            TargetKind kind,
            BlockPos blockPos,
            Entity entity,
            Component title,
            Component detail
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
                        Component.literal(entityId + " #" + entity.getId())
                );
            }

            if (hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                return new TargetSnapshot(
                        TargetKind.BLOCK,
                        pos,
                        null,
                        state.getBlock().getName(),
                        Component.literal(blockId + " @ " + pos.toShortString())
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
        ENTITY
    }
}
