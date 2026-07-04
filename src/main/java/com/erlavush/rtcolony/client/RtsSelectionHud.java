package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

final class RtsSelectionHud {
    private static final Component SELECTED_LABEL = Component.translatable("rtcolony.hud.selected_entity");
    private static final Component HEALTH_LABEL = Component.translatable("rtcolony.hud.health");
    private static final Component TYPE_LABEL = Component.translatable("rtcolony.hud.type");
    private static final Component POSITION_LABEL = Component.translatable("rtcolony.hud.position");

    private static final int PANEL_WIDTH = 236;
    private static final int PANEL_HEIGHT = 82;
    private static final int PORTRAIT_SIZE = 62;
    private static final int MARGIN = 8;

    private RtsSelectionHud() {
    }

    static void render(Minecraft minecraft, GuiGraphics guiGraphics) {
        RtsTargetingState.TargetSnapshot target = RtsTargetingState.getSelectedTarget();
        if (target == null || target.kind() != RtsTargetingState.TargetKind.ENTITY || target.entity() == null) {
            return;
        }

        Entity entity = target.entity();
        if (entity.isRemoved()) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int width = Math.min(PANEL_WIDTH, screenWidth - MARGIN * 2);
        int height = PANEL_HEIGHT;
        int x = MARGIN;
        int y = screenHeight - height - MARGIN;

        if (width < 180) {
            return;
        }

        RtsHudFrameRenderer.drawPanel(guiGraphics, x, y, width, height);

        int portraitX = x + 8;
        int portraitY = y + 11;
        RtsHudFrameRenderer.drawInset(guiGraphics, portraitX, portraitY, PORTRAIT_SIZE, PORTRAIT_SIZE);
        guiGraphics.enableScissor(portraitX + 2, portraitY + 2, portraitX + PORTRAIT_SIZE - 2, portraitY + PORTRAIT_SIZE - 2);
        try {
            RtsEntityPortraitRenderer.render(guiGraphics.pose(), entity, portraitX + 2, portraitY + 2, PORTRAIT_SIZE - 4);
        } finally {
            guiGraphics.disableScissor();
        }

        int infoX = portraitX + PORTRAIT_SIZE + 9;
        int infoWidth = x + width - infoX - 8;
        Font font = minecraft.font;
        guiGraphics.drawString(font, SELECTED_LABEL, infoX, y + 8, 0xFFBEBEBE, false);
        guiGraphics.drawString(font, trim(font, target.title().getString(), infoWidth), infoX, y + 20, 0xFFFFFFFF, false);

        if (entity instanceof LivingEntity livingEntity) {
            drawHealth(guiGraphics, font, livingEntity, infoX, y + 36, infoWidth);
        } else {
            guiGraphics.drawString(font, trim(font, target.detail().getString(), infoWidth), infoX, y + 38, 0xFFE0E0E0, false);
        }

        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String type = TYPE_LABEL.getString() + ": " + entityType;
        String position = POSITION_LABEL.getString() + ": " + entity.blockPosition().toShortString();
        guiGraphics.drawString(font, trim(font, type, infoWidth), infoX, y + 58, 0xFFBEBEBE, false);
        guiGraphics.drawString(font, trim(font, position, infoWidth), infoX, y + 68, 0xFFBEBEBE, false);
    }

    private static void drawHealth(GuiGraphics guiGraphics, Font font, LivingEntity entity, int x, int y, int width) {
        float health = Math.max(0.0F, entity.getHealth());
        float maxHealth = Math.max(1.0F, entity.getMaxHealth());
        int barWidth = Math.max(50, Math.min(112, width));
        int filledWidth = Math.round(barWidth * Mth.clamp(health / maxHealth, 0.0F, 1.0F));
        String text = HEALTH_LABEL.getString() + ": " + formatHealth(health) + "/" + formatHealth(maxHealth);

        guiGraphics.drawString(font, trim(font, text, width), x, y, 0xFFE0E0E0, false);
        int barY = y + 11;
        guiGraphics.fill(x, barY, x + barWidth, barY + 7, 0xFF101010);
        guiGraphics.fill(x + 1, barY + 1, x + barWidth - 1, barY + 6, 0xFF5A1515);
        if (filledWidth > 2) {
            guiGraphics.fill(x + 1, barY + 1, x + filledWidth - 1, barY + 6, 0xFF27A427);
        }
        guiGraphics.fill(x, barY, x + barWidth, barY + 1, 0xFFE0E0E0);
        guiGraphics.fill(x, barY, x + 1, barY + 7, 0xFFE0E0E0);
        guiGraphics.fill(x, barY + 6, x + barWidth, barY + 7, 0xFF151515);
        guiGraphics.fill(x + barWidth - 1, barY, x + barWidth, barY + 7, 0xFF151515);
    }

    private static String formatHealth(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String trim(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }

        String ellipsis = "...";
        int bodyWidth = Math.max(0, width - font.width(ellipsis));
        return font.plainSubstrByWidth(text, bodyWidth) + ellipsis;
    }
}
