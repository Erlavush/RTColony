package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/*
 * Adapted from Reign of Nether's MyRenderer frame/icon helpers.
 */
final class RtsHudFrameRenderer {
    private static final int FRAME_EDGE_THICKNESS = 4;

    private RtsHudFrameRenderer() {
    }

    static void drawFrameWithBg(GuiGraphics guiGraphics, int x, int y, int width, int height, int bgColor) {
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, bgColor);

        ResourceLocation iconFrameResource = hudTexture("unit_frame_left" + (height < 50 ? "_small" : "") + ".png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(
                iconFrameResource,
                x,
                y,
                0,
                0.0F,
                0.0F,
                FRAME_EDGE_THICKNESS,
                height,
                FRAME_EDGE_THICKNESS,
                height
        );

        iconFrameResource = hudTexture("unit_frame_right" + (height < 50 ? "_small" : "") + ".png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(
                iconFrameResource,
                x + width - FRAME_EDGE_THICKNESS,
                y,
                0,
                0.0F,
                0.0F,
                FRAME_EDGE_THICKNESS,
                height,
                FRAME_EDGE_THICKNESS,
                height
        );

        iconFrameResource = hudTexture("unit_frame_top.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(
                iconFrameResource,
                x + FRAME_EDGE_THICKNESS,
                y,
                0,
                0.0F,
                0.0F,
                width - FRAME_EDGE_THICKNESS * 2,
                FRAME_EDGE_THICKNESS,
                width,
                FRAME_EDGE_THICKNESS
        );

        iconFrameResource = hudTexture("unit_frame_bottom.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        guiGraphics.blit(
                iconFrameResource,
                x + FRAME_EDGE_THICKNESS,
                y + height - FRAME_EDGE_THICKNESS,
                0,
                0.0F,
                0.0F,
                width - FRAME_EDGE_THICKNESS * 2,
                FRAME_EDGE_THICKNESS,
                width,
                FRAME_EDGE_THICKNESS
        );
    }

    static void renderIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y, int size) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, icon);
        guiGraphics.blit(icon, x, y, 0, 0.0F, 0.0F, size, size, size, size);
    }

    static ResourceLocation itemIcon(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(RTColony.MOD_ID, "textures/icons/items/" + fileName);
    }

    private static ResourceLocation hudTexture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(RTColony.MOD_ID, "textures/hud/" + fileName);
    }
}
