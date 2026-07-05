package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/*
 * Adapted from Reign of Nether's HealthBarClientEvents GUI portrait bar.
 */
final class RtsHealthBarRenderer {
    private static final ResourceLocation GUI_BARS_TEXTURES =
            ResourceLocation.fromNamespaceAndPath(RTColony.MOD_ID, "textures/hud/healthbars.png");
    private static final int BAR_U = 0;
    private static final int BAR_V = 65;
    private static final int BAR_TEXTURE_WIDTH = 92;
    private static final int BAR_TEXTURE_HEIGHT = 5;
    private static final int TEXTURE_SIZE = 256;
    private static final int GUI_PORTRAIT_HEIGHT = 12;

    private RtsHealthBarRenderer() {
    }

    static void renderForEntity(GuiGraphics guiGraphics, LivingEntity entity, double x, double y, float width) {
        float maxHealth = Math.max(1.0F, entity.getMaxHealth());
        float percent = Math.min(1.0F, Math.max(0.0F, entity.getHealth()) / maxHealth);
        render(guiGraphics, 1.0F, x, y, width, 0.35F, 0.35F, 0.35F);
        render(guiGraphics, percent, x, y, width, barRed(percent), barGreen(percent), 0.0F);
    }

    static void renderAbsorbForEntity(GuiGraphics guiGraphics, LivingEntity entity, double x, double y, float width) {
        float absorption = entity.getAbsorptionAmount();
        if (absorption <= 0.0F) {
            return;
        }

        float percent = Math.min(1.0F, absorption / Math.max(1.0F, entity.getMaxHealth()));
        render(guiGraphics, percent, x, y, width, 0.0F, 1.0F, 1.0F);
    }

    private static void render(
            GuiGraphics guiGraphics,
            float percent,
            double x,
            double y,
            float width,
            float red,
            float green,
            float blue
    ) {
        if (percent <= 0.0F) {
            return;
        }

        int drawWidth = Mth.ceil(width * percent);
        int textureWidth = Mth.ceil(BAR_TEXTURE_WIDTH * percent);
        int drawX = Mth.floor(x - width / 2.0F);
        int drawY = Mth.floor(y);

        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_BARS_TEXTURES);
        guiGraphics.blit(
                GUI_BARS_TEXTURES,
                drawX,
                drawY,
                drawWidth,
                GUI_PORTRAIT_HEIGHT,
                (float) BAR_U,
                (float) BAR_V,
                textureWidth,
                BAR_TEXTURE_HEIGHT,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float barRed(float percent) {
        return Math.max(0.0F, Math.min(1.0F, 2.0F - percent * 2.0F));
    }

    private static float barGreen(float percent) {
        return Math.max(0.0F, Math.min(1.0F, percent * 2.0F));
    }
}
