package com.erlavush.rtcolony.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

final class RtsEntityPortraitRenderer {
    private static final float MIN_SCALE = 12.0F;
    private static final float MAX_SCALE = 42.0F;

    private RtsEntityPortraitRenderer() {
    }

    static void render(PoseStack poseStack, Entity entity, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        int renderX = x + size / 2;
        int renderY = y + size - 8;
        float scale = calculateScale(entity, size);
        float lookYaw = (float) Math.sin((minecraft.level == null ? 0L : minecraft.level.getGameTime()) / 23.0D) * 12.0F;
        float lookPitch = (float) Math.sin((minecraft.level == null ? 0L : minecraft.level.getGameTime()) / 31.0D) * 5.0F;

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRotationSnapshot snapshot = EntityRotationSnapshot.capture(entity);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        boolean posePushed = false;
        modelViewStack.pushMatrix();

        try {
            modelViewStack.translate((float) renderX, (float) renderY, 1050.0F);
            modelViewStack.scale(1.0F, 1.0F, -1.0F);
            RenderSystem.applyModelViewMatrix();

            poseStack.pushPose();
            posePushed = true;
            poseStack.translate(0.0D, 0.0D, 100.0D);
            poseStack.scale(scale, scale, scale);

            Quaternionf baseRotation = Axis.ZP.rotationDegrees(180.0F);
            Quaternionf pitchRotation = Axis.XP.rotationDegrees(lookPitch);
            baseRotation.mul(pitchRotation);
            poseStack.mulPose(baseRotation);

            applyPortraitRotation(entity, lookYaw, lookPitch);

            Lighting.setupForEntityInInventory();
            dispatcher.setRenderShadow(false);
            pitchRotation.conjugate();
            dispatcher.overrideCameraOrientation(pitchRotation);

            RenderSystem.runAsFancy(() -> {
                try {
                    MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
                    dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, bufferSource, 15728880);
                    bufferSource.endBatch();
                } catch (RuntimeException ignored) {
                    // Some modded renderers can fail in GUI space; keep the HUD from crashing the client.
                }
            });
        } finally {
            dispatcher.setRenderShadow(true);
            snapshot.restore(entity);
            modelViewStack.popMatrix();
            if (posePushed) {
                poseStack.popPose();
            }
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
        }
    }

    private static float calculateScale(Entity entity, int size) {
        double width = Math.max(entity.getBbWidth(), 0.4D);
        double height = Math.max(entity.getBbHeight(), 0.4D);
        double largest = Math.max(width, height);
        return (float) Mth.clamp(size / largest * 0.65D, MIN_SCALE, MAX_SCALE);
    }

    private static void applyPortraitRotation(Entity entity, float lookYaw, float lookPitch) {
        entity.setYRot(180.0F + lookYaw);
        entity.setXRot(-lookPitch);

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.yBodyRot = 180.0F + lookYaw * 0.5F;
            livingEntity.yBodyRotO = livingEntity.yBodyRot;
            livingEntity.yHeadRot = entity.getYRot();
            livingEntity.yHeadRotO = entity.getYRot();
        }
    }

    private record EntityRotationSnapshot(
            float yRot,
            float xRot,
            float yBodyRot,
            float yBodyRotO,
            float yHeadRot,
            float yHeadRotO
    ) {
        private static EntityRotationSnapshot capture(Entity entity) {
            if (entity instanceof LivingEntity livingEntity) {
                return new EntityRotationSnapshot(
                        entity.getYRot(),
                        entity.getXRot(),
                        livingEntity.yBodyRot,
                        livingEntity.yBodyRotO,
                        livingEntity.yHeadRot,
                        livingEntity.yHeadRotO
                );
            }

            return new EntityRotationSnapshot(entity.getYRot(), entity.getXRot(), 0.0F, 0.0F, 0.0F, 0.0F);
        }

        private void restore(Entity entity) {
            entity.setYRot(this.yRot);
            entity.setXRot(this.xRot);

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.yBodyRot = this.yBodyRot;
                livingEntity.yBodyRotO = this.yBodyRotO;
                livingEntity.yHeadRot = this.yHeadRot;
                livingEntity.yHeadRotO = this.yHeadRotO;
            }
        }
    }
}
