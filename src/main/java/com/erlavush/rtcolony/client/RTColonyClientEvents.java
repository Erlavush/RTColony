package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class RTColonyClientEvents {
    private static final Component RTS_MODE_LABEL = Component.translatable("rtcolony.hud.rts_mode");

    private RTColonyClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (RTColonyKeyMappings.TOGGLE_RTS_MODE.consumeClick()) {
            RtsModeState.toggle();
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
}
