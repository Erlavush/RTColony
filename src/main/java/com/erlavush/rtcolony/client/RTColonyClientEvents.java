package com.erlavush.rtcolony.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class RTColonyClientEvents {
    private static final int EDGE_PAN_PIXELS = 8;
    private static boolean enableRtsModeOnNextWorld = true;

    private RTColonyClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (RTColonyKeyMappings.OPEN_CONFIG.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new RTColonyConfigScreen(null));
            }
        }

        while (RTColonyKeyMappings.CYCLE_CAMERA_MODE.consumeClick()) {
            enableRtsModeOnNextWorld = false;
            if (RtsModeState.isEnabled()) {
                RtsCameraState.cycleMode();
            } else {
                RtsModeState.setEnabled(true);
            }
        }

        if (!RtsModeState.isEnabled()) {
            RtsCutawayState.clear();
            RtsSodiumCutaway.clear();

            if (enableRtsModeOnNextWorld
                    && minecraft.player != null
                    && minecraft.level != null
                    && minecraft.screen == null) {
                enableRtsModeOnNextWorld = false;
                RtsCameraState.setMode(RtsCameraMode.PERSPECTIVE);
                RtsModeState.setEnabled(true);
            } else {
                return;
            }
        }

        if (minecraft.player == null || minecraft.level == null) {
            RtsCutawayState.clear();
            RtsSodiumCutaway.clear();
            RtsModeState.setEnabled(false);
            return;
        }

        RtsCameraState.ensureActive(minecraft.player);
        if (minecraft.screen == null) {
            minecraft.mouseHandler.releaseMouse();
            updateEdgePanning(minecraft);
        }

        boolean followingTarget =
                RtsTargetingState.tickFollow(minecraft);

        if (!followingTarget
                && !RtsBuildDrawer.isPlacementLocked()
                && !RtsCameraState.isTrueIsometric()) {
            RtsCameraState.updateTerrainHeight(
                    minecraft.level,
                    RTColonyClientConfig.get(minecraft)
                            .terrainStabilizationEnabled()
            );
        }

        RtsEntityPortraitRenderer.tick();

        if (minecraft.screen == null) {
            RtsTargetingState.updateHover(minecraft);
            RtsBuildDrawer.tick(minecraft);
        }

        RtsCutawayState.tick(minecraft);
        RtsSodiumCutaway.tick(minecraft);
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!RtsModeState.isEnabled()) {
            return;
        }

        Input input = event.getInput();
        input.left = false;
        input.right = false;
        input.up = false;
        input.down = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!RtsModeState.isEnabled() || Minecraft.getInstance().screen != null) {
            return;
        }

        RtsCameraState.zoom(event.getScrollDeltaY());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (RtsModeState.isEnabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (RtsModeState.isEnabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RtsModeState.isEnabled() || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Component modeLabel = rtsModeLabel();
        int textWidth = minecraft.font.width(modeLabel);
        guiGraphics.fill(6, 6, textWidth + 14, 22, 0xA0000000);
        guiGraphics.drawString(minecraft.font, modeLabel, 10, 10, 0xFFFFFF, false);
        RtsSelectionHud.render(minecraft, guiGraphics);
        RtsBuildDrawer.render(minecraft, guiGraphics);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!RtsModeState.isEnabled() || !isHiddenVanillaHudLayer(event.getName())) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!RtsModeState.isEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        RtsTargetingState.updateHover(minecraft);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        RtsTargetingState.TargetSnapshot selected = RtsTargetingState.getSelectedTarget();
        if (selected != null) {
            drawTargetOutline(poseStack, consumer, camera, selected, 0.2F, 0.95F, 1.0F, 1.0F);
        }

        RtsTargetingState.TargetSnapshot hovered = RtsTargetingState.getHoveredTarget();
        if (hovered != null && !hovered.sameTarget(selected)) {
            drawTargetOutline(poseStack, consumer, camera, hovered, 1.0F, 0.9F, 0.25F, 1.0F);
        }

        RtsBuildDrawer.renderPlacementGrid(poseStack, consumer, camera);

        bufferSource.endBatch(RenderType.lines());
    }

    private static Component rtsModeLabel() {
        return Component.translatable(RtsCameraState.isTrueIsometric()
                ? "rtcolony.hud.rts_mode.isometric"
                : "rtcolony.hud.rts_mode.perspective");
    }

    private static void updateEdgePanning(Minecraft minecraft) {
        RTColonyClientConfig.Config config = RTColonyClientConfig.get(minecraft);
        if (!minecraft.isWindowActive()
                || !config.edgePanningEnabled()
                || minecraft.mouseHandler.isLeftPressed()
                || minecraft.mouseHandler.isMiddlePressed()
                || minecraft.mouseHandler.isRightPressed()
                || RtsBuildDrawer.isPlacementLocked()
                || RtsBuildDrawer.isMouseOver(minecraft)
                || RtsSelectionHud.isMouseOver(minecraft)) {
            return;
        }

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        int width = minecraft.getWindow().getScreenWidth();
        int height = minecraft.getWindow().getScreenHeight();

        float leftImpulse = 0.0F;
        float forwardImpulse = 0.0F;
        if (mouseX <= EDGE_PAN_PIXELS) {
            leftImpulse += 1.0F;
        } else if (mouseX >= width - EDGE_PAN_PIXELS) {
            leftImpulse -= 1.0F;
        }
        if (mouseY <= EDGE_PAN_PIXELS) {
            forwardImpulse += 1.0F;
        } else if (mouseY >= height - EDGE_PAN_PIXELS) {
            forwardImpulse -= 1.0F;
        }

        RtsCameraState.pan(leftImpulse, forwardImpulse, config.edgePanningSensitivity());
    }

    private static boolean isHiddenVanillaHudLayer(ResourceLocation layerName) {
        return VanillaGuiLayers.HOTBAR.equals(layerName)
                || VanillaGuiLayers.CROSSHAIR.equals(layerName)
                || VanillaGuiLayers.SELECTED_ITEM_NAME.equals(layerName)
                || VanillaGuiLayers.EXPERIENCE_BAR.equals(layerName)
                || VanillaGuiLayers.EXPERIENCE_LEVEL.equals(layerName)
                || VanillaGuiLayers.JUMP_METER.equals(layerName)
                || VanillaGuiLayers.SPECTATOR_TOOLTIP.equals(layerName);
    }

    private static void drawTargetOutline(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 camera,
            RtsTargetingState.TargetSnapshot target,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        AABB outline = target.outlineBox();
        if (outline == null) {
            return;
        }

        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                outline.move(-camera.x, -camera.y, -camera.z),
                red,
                green,
                blue,
                alpha
        );
    }
}
