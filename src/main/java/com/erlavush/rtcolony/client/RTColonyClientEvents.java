package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class RTColonyClientEvents {
    private static final Component RTS_MODE_LABEL = Component.translatable("rtcolony.hud.rts_mode");
    private static final int EDGE_PAN_PIXELS = 8;

    private RTColonyClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (RTColonyKeyMappings.TOGGLE_RTS_MODE.consumeClick()) {
            RtsModeState.toggle();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!RtsModeState.isEnabled()) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            RtsModeState.setEnabled(false);
            return;
        }

        RtsCameraState.ensureActive(minecraft.player);
        RtsCameraState.updateTerrainHeight(minecraft.level);

        if (minecraft.screen == null) {
            minecraft.mouseHandler.releaseMouse();
            updateRotationKeys();
            updateEdgePanning(minecraft);
        }
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!RtsModeState.isEnabled()) {
            return;
        }

        Input input = event.getInput();
        RtsCameraState.pan(input.leftImpulse, input.forwardImpulse);
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
        int textWidth = minecraft.font.width(RTS_MODE_LABEL);
        guiGraphics.fill(6, 6, textWidth + 14, 22, 0xA0000000);
        guiGraphics.drawString(minecraft.font, RTS_MODE_LABEL, 10, 10, 0xFFFFFF, false);
    }

    private static void updateRotationKeys() {
        if (RTColonyKeyMappings.ROTATE_CAMERA_LEFT.isDown()) {
            RtsCameraState.rotateLeft();
        }
        if (RTColonyKeyMappings.ROTATE_CAMERA_RIGHT.isDown()) {
            RtsCameraState.rotateRight();
        }
    }

    private static void updateEdgePanning(Minecraft minecraft) {
        if (!minecraft.isWindowActive()) {
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

        RtsCameraState.pan(leftImpulse, forwardImpulse);
    }
}
