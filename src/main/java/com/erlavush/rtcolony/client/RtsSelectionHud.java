package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/*
 * Selected unit portrait HUD adapted from Reign of Nether's PortraitRendererUnit,
 * MyRenderer, and HealthBarClientEvents.
 */
public final class RtsSelectionHud {
    private static final int PORTRAIT_FRAME_SIZE = 60;
    private static final int STATS_FRAME_WIDTH = 45;
    private static final int INFO_FRAME_WIDTH = 154;
    private static final int FRAME_HEIGHT = 60;
    private static final int MARGIN = 8;
    private static final int STAT_ICON_SIZE = 8;
    private static final int ACTION_BUTTON_WIDTH = 80;
    private static final int ACTION_BUTTON_HEIGHT = 18;

    private RtsSelectionHud() {
    }

    static void render(Minecraft minecraft, GuiGraphics guiGraphics) {
        RtsTargetingState.TargetSnapshot target = RtsTargetingState.getSelectedTarget();
        if (target == null) {
            return;
        }

        Optional<RtsMineColoniesIntegration.SelectionInfo> mineColoniesInfo =
                RtsMineColoniesIntegration.createInfo(minecraft, target);

        if (target.kind() == RtsTargetingState.TargetKind.BLOCK
                || target.kind() == RtsTargetingState.TargetKind.BUILDING) {
            mineColoniesInfo.ifPresent(info -> {
                renderMineColoniesBlockInfo(minecraft, guiGraphics, info);
                renderActions(minecraft, guiGraphics, actionLayout(minecraft, target, true));
            });
            return;
        }

        if (target.entity() == null) {
            return;
        }

        Entity entity = target.entity();
        if (entity.isRemoved() || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int x = MARGIN;
        int y = screenHeight - FRAME_HEIGHT - MARGIN;
        Font font = minecraft.font;
        String name = capitalize(target.title().getString().replace("_", " "));

        RtsHudFrameRenderer.drawFrameWithBg(guiGraphics, x, y, PORTRAIT_FRAME_SIZE, FRAME_HEIGHT, 0xA0000000);
        RtsEntityPortraitRenderer.render(guiGraphics, livingEntity, x, y);

        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0.0D, 0.0D, 2000.0D);
            guiGraphics.drawString(font, name, x + 4, y - 9, 0xFFFFFFFF, false);
            RtsHealthBarRenderer.renderForEntity(
                    guiGraphics,
                    livingEntity,
                    x + PORTRAIT_FRAME_SIZE / 2.0F,
                    y + FRAME_HEIGHT - 14.0F,
                    PORTRAIT_FRAME_SIZE - 9.0F
            );
            RtsHealthBarRenderer.renderAbsorbForEntity(
                    guiGraphics,
                    livingEntity,
                    x + PORTRAIT_FRAME_SIZE / 2.0F,
                    y + FRAME_HEIGHT - 14.0F,
                    PORTRAIT_FRAME_SIZE - 9.0F
            );
            renderHealthText(guiGraphics, font, livingEntity, x, y);
        } finally {
            guiGraphics.pose().popPose();
        }

        renderStats(minecraft, guiGraphics, livingEntity, x + PORTRAIT_FRAME_SIZE, y);
        mineColoniesInfo.ifPresent(info -> renderMineColoniesEntityInfo(
                minecraft,
                guiGraphics,
                info,
                x + PORTRAIT_FRAME_SIZE + STATS_FRAME_WIDTH,
                y
        ));
        renderActions(minecraft, guiGraphics, actionLayout(minecraft, target, mineColoniesInfo.isPresent()));
    }

    public static boolean handleMousePress(
            Minecraft minecraft,
            int button,
            int action,
            double rawMouseX,
            double rawMouseY
    ) {
        if (!RtsModeState.isEnabled()
                || minecraft.options.hideGui
                || minecraft.screen != null
                || action != GLFW.GLFW_PRESS) {
            return false;
        }

        RtsTargetingState.TargetSnapshot target = RtsTargetingState.getSelectedTarget();
        if (target == null) {
            return false;
        }
        if (target.kind() == RtsTargetingState.TargetKind.ENTITY
                && (target.entity() == null
                || target.entity().isRemoved()
                || !(target.entity() instanceof LivingEntity))) {
            return false;
        }

        Optional<RtsMineColoniesIntegration.SelectionInfo> info =
                RtsMineColoniesIntegration.createInfo(minecraft, target);
        ActionLayout actions = actionLayout(minecraft, target, info.isPresent());
        int mouseX = scaledMouseX(minecraft, rawMouseX);
        int mouseY = scaledMouseY(minecraft, rawMouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && actions != null) {
            if (actions.followButton() != null && actions.followButton().contains(mouseX, mouseY)) {
                RtsTargetingState.toggleFollowSelected();
                return true;
            }
            if (actions.detailsButton() != null && actions.detailsButton().contains(mouseX, mouseY)) {
                RtsMineColoniesIntegration.openNativeDetails(minecraft, target);
                return true;
            }
        }
        return isMouseOver(minecraft, mouseX, mouseY, target, info.isPresent());
    }

    static boolean isMouseOver(Minecraft minecraft) {
        RtsTargetingState.TargetSnapshot target = RtsTargetingState.getSelectedTarget();
        if (!RtsModeState.isEnabled()
                || minecraft.options.hideGui
                || minecraft.screen != null
                || target == null) {
            return false;
        }
        boolean hasInfo = RtsMineColoniesIntegration.createInfo(minecraft, target).isPresent();
        return isMouseOver(minecraft, scaledMouseX(minecraft), scaledMouseY(minecraft), target, hasInfo);
    }

    private static boolean isMouseOver(
            Minecraft minecraft,
            int mouseX,
            int mouseY,
            RtsTargetingState.TargetSnapshot target,
            boolean hasInfo
    ) {
        int y = minecraft.getWindow().getGuiScaledHeight() - FRAME_HEIGHT - MARGIN;
        int width;
        if (target.kind() == RtsTargetingState.TargetKind.ENTITY) {
            if (target.entity() == null
                    || target.entity().isRemoved()
                    || !(target.entity() instanceof LivingEntity)) {
                return false;
            }
            width = PORTRAIT_FRAME_SIZE + STATS_FRAME_WIDTH + (hasInfo ? INFO_FRAME_WIDTH : 0);
        } else if (hasInfo) {
            width = INFO_FRAME_WIDTH;
        } else {
            return false;
        }
        return mouseX >= MARGIN && mouseY >= y - 33 && mouseX < MARGIN + width && mouseY < y + FRAME_HEIGHT;
    }

    private static ActionLayout actionLayout(
            Minecraft minecraft,
            RtsTargetingState.TargetSnapshot target,
            boolean hasDetails
    ) {
        int y = minecraft.getWindow().getGuiScaledHeight() - FRAME_HEIGHT - MARGIN;
        if (target.kind() == RtsTargetingState.TargetKind.ENTITY) {
            if (hasDetails) {
                int x = MARGIN + PORTRAIT_FRAME_SIZE + STATS_FRAME_WIDTH;
                int width = (INFO_FRAME_WIDTH - 4) / 2;
                return new ActionLayout(
                        new Rect(x, y - 32, width, ACTION_BUTTON_HEIGHT),
                        new Rect(x + width + 4, y - 32, width, ACTION_BUTTON_HEIGHT)
                );
            }
            int x = MARGIN + (PORTRAIT_FRAME_SIZE + STATS_FRAME_WIDTH - ACTION_BUTTON_WIDTH) / 2;
            return new ActionLayout(new Rect(x, y - 32, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT), null);
        }
        if (target.kind() == RtsTargetingState.TargetKind.BUILDING && hasDetails) {
            int x = MARGIN + (INFO_FRAME_WIDTH - ACTION_BUTTON_WIDTH) / 2;
            return new ActionLayout(null, new Rect(x, y - 32, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT));
        }
        return null;
    }

    private static void renderActions(Minecraft minecraft, GuiGraphics guiGraphics, ActionLayout layout) {
        if (layout == null) {
            return;
        }
        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);
        if (layout.followButton() != null) {
            drawButton(
                    minecraft,
                    guiGraphics,
                    layout.followButton(),
                    Component.translatable(RtsTargetingState.isFollowingSelected()
                            ? "rtcolony.selection.stop_follow"
                            : "rtcolony.selection.follow"),
                    layout.followButton().contains(mouseX, mouseY)
            );
        }
        if (layout.detailsButton() != null) {
            drawButton(
                    minecraft,
                    guiGraphics,
                    layout.detailsButton(),
                    Component.translatable("rtcolony.selection.open_details"),
                    layout.detailsButton().contains(mouseX, mouseY)
            );
        }
    }

    private static void drawButton(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            Rect rect,
            Component label,
            boolean hovered
    ) {
        guiGraphics.fill(
                rect.x(),
                rect.y(),
                rect.x() + rect.width(),
                rect.y() + rect.height(),
                hovered ? 0xFFE2BD64 : 0xFF777777
        );
        guiGraphics.fill(
                rect.x() + 1,
                rect.y() + 1,
                rect.x() + rect.width() - 1,
                rect.y() + rect.height() - 1,
                hovered ? 0xCC594A2C : 0xCC303030
        );
        String text = trim(minecraft.font, label.getString(), rect.width() - 6);
        guiGraphics.drawString(
                minecraft.font,
                text,
                rect.x() + (rect.width() - minecraft.font.width(text)) / 2,
                rect.y() + 5,
                0xFFFFFFFF,
                false
        );
    }

    private static void renderHealthText(GuiGraphics guiGraphics, Font font, LivingEntity entity, int x, int y) {
        float health = Math.max(0.0F, entity.getHealth()) + entity.getAbsorptionAmount();
        float maxHealth = entity.getMaxHealth() + entity.getAbsorptionAmount();
        String text = formatNumber(health) + "/" + formatNumber(maxHealth);
        int textX = x + PORTRAIT_FRAME_SIZE / 2 - font.width(text) / 2;
        guiGraphics.drawString(font, text, textX, y + FRAME_HEIGHT - 12, 0xFFFFFFFF, false);
    }

    private static void renderStats(Minecraft minecraft, GuiGraphics guiGraphics, LivingEntity entity, int x, int y) {
        RtsHudFrameRenderer.drawFrameWithBg(guiGraphics, x, y, STATS_FRAME_WIDTH, FRAME_HEIGHT, 0xA0000000);

        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);
        int iconX = x + 6;
        int iconY = y + 7;
        List<RenderedStat> renderedStats = List.of(
                new RenderedStat(
                        RtsHudFrameRenderer.itemIcon("sword.png"),
                        formatNumber(attributeValue(entity, Attributes.ATTACK_DAMAGE)),
                        Component.literal("Attack damage"),
                        0xFFFFFFFF,
                        0
                ),
                new RenderedStat(
                        RtsHudFrameRenderer.itemIcon("chestplate.png"),
                        Integer.toString(entity.getArmorValue()),
                        Component.literal("Armor"),
                        entity.getArmorValue() > 0 ? 0xFF2BFF2B : 0xFFFFFFFF,
                        0
                ),
                new RenderedStat(
                        RtsHudFrameRenderer.itemIcon("boots.png"),
                        Integer.toString(movementSpeed(entity)),
                        Component.literal("Movement speed"),
                        0xFFFFFFFF,
                        0
                )
        );

        for (RenderedStat renderedStat : renderedStats) {
            boolean isMouseOver = mouseX >= iconX - 1
                    && mouseY >= iconY - 1
                    && mouseX < iconX + STATS_FRAME_WIDTH - 11
                    && mouseY < iconY + 9;
            if (isMouseOver) {
                guiGraphics.fill(iconX - 1, iconY - 1, iconX + STATS_FRAME_WIDTH - 11, iconY + 9, 0x32FFFFFF);
            }

            RtsHudFrameRenderer.renderIcon(guiGraphics, renderedStat.icon(), iconX, iconY, STAT_ICON_SIZE);
            guiGraphics.drawString(
                    minecraft.font,
                    renderedStat.text(),
                    iconX + 13 + renderedStat.textXOffset(),
                    iconY,
                    renderedStat.color(),
                    false
            );

            if (isMouseOver) {
                guiGraphics.renderTooltip(minecraft.font, renderedStat.tooltip(), mouseX, mouseY);
            }

            iconY += 10;
        }
    }

    private static void renderMineColoniesEntityInfo(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            RtsMineColoniesIntegration.SelectionInfo info,
            int x,
            int y
    ) {
        renderMineColoniesInfoPanel(minecraft, guiGraphics, info, x, y, INFO_FRAME_WIDTH);
    }

    private static void renderMineColoniesBlockInfo(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            RtsMineColoniesIntegration.SelectionInfo info
    ) {
        int x = MARGIN;
        int y = minecraft.getWindow().getGuiScaledHeight() - FRAME_HEIGHT - MARGIN;
        renderMineColoniesInfoPanel(minecraft, guiGraphics, info, x, y, INFO_FRAME_WIDTH);
    }

    private static void renderMineColoniesInfoPanel(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            RtsMineColoniesIntegration.SelectionInfo info,
            int x,
            int y,
            int width
    ) {
        Font font = minecraft.font;
        RtsHudFrameRenderer.drawFrameWithBg(guiGraphics, x, y, width, FRAME_HEIGHT, 0xA0000000);
        guiGraphics.drawString(font, trim(font, info.title(), width - 8), x + 4, y - 9, 0xFFFFFFFF, false);

        int textX = x + 7;
        int textY = y + 6;
        int textWidth = width - 14;
        for (RtsMineColoniesIntegration.Line line : info.lines()) {
            String text = line.label() + ": " + line.value();
            guiGraphics.drawString(font, trim(font, text, textWidth), textX, textY, 0xFFE0E0E0, false);
            textY += 10;
            if (textY > y + FRAME_HEIGHT - 9) {
                break;
            }
        }
    }

    private static double attributeValue(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getValue();
    }

    private static int movementSpeed(LivingEntity entity) {
        double speed = attributeValue(entity, Attributes.MOVEMENT_SPEED);
        int speedText = (int) (speed * 101.0D);
        if (entity instanceof Slime) {
            speedText = (int) (speedText * 0.45F);
        }
        return speedText;
    }

    private static int scaledMouseX(Minecraft minecraft) {
        return (int) (minecraft.mouseHandler.xpos()
                * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth());
    }

    private static int scaledMouseY(Minecraft minecraft) {
        return (int) (minecraft.mouseHandler.ypos()
                * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight());
    }

    private static int scaledMouseX(Minecraft minecraft, double rawMouseX) {
        return (int) (rawMouseX
                * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth());
    }

    private static int scaledMouseY(Minecraft minecraft, double rawMouseY) {
        return (int) (rawMouseY
                * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight());
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.05D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static String trim(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }

        String ellipsis = "...";
        int bodyWidth = Math.max(0, width - font.width(ellipsis));
        return font.plainSubstrByWidth(text, bodyWidth) + ellipsis;
    }

    private record RenderedStat(
            net.minecraft.resources.ResourceLocation icon,
            String text,
            Component tooltip,
            int color,
            int textXOffset
    ) {
    }

    private record ActionLayout(Rect followButton, Rect detailsButton) {
    }

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x
                    && mouseY >= this.y
                    && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;
        }
    }
}
