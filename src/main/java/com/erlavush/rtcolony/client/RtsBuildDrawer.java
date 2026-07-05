package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.storage.rendering.RenderingCache;
import com.ldtteam.structurize.storage.rendering.types.BlueprintPreviewData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class RtsBuildDrawer {
    private static final String PREVIEW_KEY = RTColony.MOD_ID + ":starter_supply";
    private static final int PAPER_TEXTURE_WIDTH = 190;
    private static final int PAPER_TEXTURE_HEIGHT = 244;
    private static final int PAPER_SHORT_TEXTURE_HEIGHT = 130;
    private static final int PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 314;
    private static final int COLLAPSED_VISIBLE_WIDTH = 38;
    private static final int COLLAPSED_HEIGHT = 150;
    private static final int ENTRY_WIDTH = 86;
    private static final int ENTRY_HEIGHT = 17;

    private static final ResourceLocation PAPER = mineColoniesTexture("builder_paper.png");
    private static final ResourceLocation PAPER_SHORT = mineColoniesTexture("builder_paper_short.png");
    private static final ResourceLocation TAB_BUTTON = mineColoniesTexture("builder_button_medium.png");
    private static final ResourceLocation CLOSE_ARROW = mineColoniesTexture("turn_page_left.png");
    private static final ResourceLocation OPEN_ARROW = mineColoniesTexture("turn_page_right.png");
    private static final ResourceLocation ENTRY_BUTTON = structurizeTexture("button_blueprint.png");
    private static final ResourceLocation ENTRY_BUTTON_SELECTED = structurizeTexture("button_blueprint_selected.png");

    private static boolean open = true;
    private static StarterEntry selectedEntry = StarterEntry.SUPPLY_CAMP;
    private static boolean previewActive;

    private RtsBuildDrawer() {
    }

    public static void toggle() {
        open = !open;
    }

    static void tick(Minecraft minecraft) {
        if (previewActive) {
            updatePreviewPosition(minecraft);
        }
    }

    static void clearPreview() {
        previewActive = false;
        RenderingCache.removeBlueprint(PREVIEW_KEY);
    }

    static boolean isMouseOver(Minecraft minecraft) {
        if (!RtsModeState.isEnabled() || minecraft.screen != null) {
            return false;
        }

        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);
        if (open) {
            return isInside(mouseX, mouseY, panelX(), panelY(minecraft), PANEL_WIDTH, PANEL_HEIGHT);
        }
        return isInside(mouseX, mouseY, collapsedX(), collapsedY(minecraft), COLLAPSED_VISIBLE_WIDTH, COLLAPSED_HEIGHT);
    }

    public static boolean handleMousePress(Minecraft minecraft, int button, int action, double rawMouseX, double rawMouseY) {
        if (!RtsModeState.isEnabled()
                || minecraft.screen != null
                || button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || action != GLFW.GLFW_PRESS) {
            return false;
        }

        int mouseX = scaledMouseX(minecraft, rawMouseX);
        int mouseY = scaledMouseY(minecraft, rawMouseY);

        if (!open) {
            if (isInside(mouseX, mouseY, collapsedX(), collapsedY(minecraft), COLLAPSED_VISIBLE_WIDTH, COLLAPSED_HEIGHT)) {
                open = true;
                return true;
            }
            return false;
        }

        int x = panelX();
        int y = panelY(minecraft);
        if (!isInside(mouseX, mouseY, x, y, PANEL_WIDTH, PANEL_HEIGHT)) {
            return false;
        }

        if (isInside(mouseX, mouseY, x + 162, y + 10, 22, 18)) {
            open = false;
            return true;
        }

        int entryX = x + 17;
        if (isInside(mouseX, mouseY, entryX, y + 76, ENTRY_WIDTH, ENTRY_HEIGHT)) {
            selectEntry(minecraft, StarterEntry.SUPPLY_CAMP);
            return true;
        }
        if (isInside(mouseX, mouseY, entryX, y + 99, ENTRY_WIDTH, ENTRY_HEIGHT)) {
            selectEntry(minecraft, StarterEntry.SUPPLY_SHIP);
            return true;
        }

        return true;
    }

    static void render(Minecraft minecraft, GuiGraphics guiGraphics) {
        if (!RtsModeState.isEnabled()) {
            return;
        }

        if (open) {
            renderOpen(minecraft, guiGraphics);
        } else {
            renderCollapsed(minecraft, guiGraphics);
        }
    }

    private static void renderOpen(Minecraft minecraft, GuiGraphics guiGraphics) {
        int x = panelX();
        int y = panelY(minecraft);
        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);
        Font font = minecraft.font;

        drawScaledTexture(guiGraphics, PAPER, x, y, PAPER_TEXTURE_WIDTH, PAPER_TEXTURE_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        drawTexture(guiGraphics, CLOSE_ARROW, x + 164, y + 14, 18, 10, 18, 10);

        guiGraphics.drawString(font, Component.translatable("rtcolony.build_drawer.title"), x + 16, y + 13, 0xFF3C2818, false);

        drawTexture(guiGraphics, TAB_BUTTON, x + 16, y + 35, 86, 17, 86, 17);
        guiGraphics.drawString(font, Component.translatable("rtcolony.build_drawer.tab.starter"), x + 43, y + 40, 0xFF211509, false);

        guiGraphics.drawString(font, Component.translatable("rtcolony.build_drawer.section.supplies"), x + 18, y + 62, 0xFF5A3A1F, false);
        renderEntry(
                guiGraphics,
                font,
                x + 17,
                y + 76,
                mouseX,
                mouseY,
                StarterEntry.SUPPLY_CAMP,
                Component.translatable("rtcolony.build_drawer.supply_camp")
        );
        renderEntry(
                guiGraphics,
                font,
                x + 17,
                y + 99,
                mouseX,
                mouseY,
                StarterEntry.SUPPLY_SHIP,
                Component.translatable("rtcolony.build_drawer.supply_ship")
        );

        guiGraphics.drawString(font, Component.translatable("rtcolony.build_drawer.preview_hint"), x + 18, y + 139, 0xFF5A3A1F, false);
        guiGraphics.drawString(font, selectedEntry.label(), x + 18, y + 153, 0xFF3C2818, false);
        guiGraphics.drawString(
                font,
                Component.translatable(previewActive ? "rtcolony.build_drawer.preview_active" : "rtcolony.build_drawer.preview_inactive"),
                x + 18,
                y + 167,
                0xFF7F6041,
                false
        );
    }

    private static void renderEntry(
            GuiGraphics guiGraphics,
            Font font,
            int x,
            int y,
            int mouseX,
            int mouseY,
            StarterEntry entry,
            Component label
    ) {
        boolean selected = selectedEntry == entry;
        boolean hovered = isInside(mouseX, mouseY, x, y, ENTRY_WIDTH, ENTRY_HEIGHT);
        drawTexture(guiGraphics, selected || hovered ? ENTRY_BUTTON_SELECTED : ENTRY_BUTTON, x, y, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
        guiGraphics.drawString(font, label, x + 7, y + 5, selected ? 0xFF101010 : 0xFF2E2215, false);
    }

    private static void renderCollapsed(Minecraft minecraft, GuiGraphics guiGraphics) {
        int x = -PANEL_WIDTH + COLLAPSED_VISIBLE_WIDTH;
        int y = collapsedY(minecraft);
        drawScaledTexture(guiGraphics, PAPER_SHORT, x, y, PANEL_WIDTH, PAPER_SHORT_TEXTURE_HEIGHT, PANEL_WIDTH, COLLAPSED_HEIGHT);
        drawVerticalLabel(guiGraphics, minecraft.font, Component.translatable("rtcolony.build_drawer.title"), 23, y + 94);
        drawTexture(guiGraphics, OPEN_ARROW, 10, y + COLLAPSED_HEIGHT - 25, 18, 10, 18, 10);
    }

    private static void drawVerticalLabel(GuiGraphics guiGraphics, Font font, Component label, int x, int y) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        guiGraphics.drawString(font, label, 0, 0, 0xFF3C2818, false);
        poseStack.popPose();
    }

    private static void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        guiGraphics.blit(texture, x, y, 0, 0.0F, 0.0F, width, height, textureWidth, textureHeight);
    }

    private static void drawScaledTexture(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int sourceWidth,
            int sourceHeight,
            int targetWidth,
            int targetHeight
    ) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        poseStack.scale((float) targetWidth / sourceWidth, (float) targetHeight / sourceHeight, 1.0F);
        drawTexture(guiGraphics, texture, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        poseStack.popPose();
    }

    private static void selectEntry(Minecraft minecraft, StarterEntry entry) {
        selectedEntry = entry;
        loadPreview(minecraft);
    }

    private static void loadPreview(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }

        try {
            StructurePacks.ensureSelectedPack();
            StructurePackMeta structurePack = StructurePacks.getSelectedPack();
            if (structurePack == null) {
                previewActive = false;
                RenderingCache.removeBlueprint(PREVIEW_KEY);
                RTColony.LOGGER.warn("Unable to load {} preview: Structurize has no selected structure pack", selectedEntry.type);
                return;
            }

            BlueprintPreviewData previewData = RenderingCache.getOrCreateBlueprintPreviewData(PREVIEW_KEY);
            previewData.setPos(previewPosition(minecraft));
            previewData.setBlueprint(null);
            previewData.setBlueprintFuture(StructurePacks.getBlueprintFuture(
                    structurePack.getName(),
                    selectedEntry.blueprintPath(),
                    minecraft.level.registryAccess()
            ));
            previewData.setRenderBlocksNice(true);
            previewData.setOverridePreviewTransparency(0.55F);
            RenderingCache.queue(PREVIEW_KEY, previewData);
            previewActive = true;
        } catch (RuntimeException exception) {
            previewActive = false;
            RenderingCache.removeBlueprint(PREVIEW_KEY);
            RTColony.LOGGER.warn("Unable to load {} Structurize preview", selectedEntry.type, exception);
        }
    }

    private static void updatePreviewPosition(Minecraft minecraft) {
        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData != null) {
            previewData.setPos(previewPosition(minecraft));
        }
    }

    private static BlockPos previewPosition(Minecraft minecraft) {
        HitResult hoverHit = RtsTargetingState.getHoverHit();
        if (hoverHit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }
        if (minecraft.player != null) {
            return minecraft.player.blockPosition();
        }
        return BlockPos.ZERO;
    }

    private static int panelX() {
        return 4;
    }

    private static int panelY(Minecraft minecraft) {
        return Math.max(26, (minecraft.getWindow().getGuiScaledHeight() - PANEL_HEIGHT) / 2);
    }

    private static int collapsedX() {
        return 0;
    }

    private static int collapsedY(Minecraft minecraft) {
        return Math.max(38, (minecraft.getWindow().getGuiScaledHeight() - COLLAPSED_HEIGHT) / 2);
    }

    private static int scaledMouseX(Minecraft minecraft) {
        return scaledMouseX(minecraft, minecraft.mouseHandler.xpos());
    }

    private static int scaledMouseY(Minecraft minecraft) {
        return scaledMouseY(minecraft, minecraft.mouseHandler.ypos());
    }

    private static int scaledMouseX(Minecraft minecraft, double rawMouseX) {
        return (int) (rawMouseX * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
    }

    private static int scaledMouseY(Minecraft minecraft, double rawMouseY) {
        return (int) (rawMouseY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static ResourceLocation mineColoniesTexture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(RTColony.MOD_ID, "textures/gui/minecolonies/builderhut/" + fileName);
    }

    private static ResourceLocation structurizeTexture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(RTColony.MOD_ID, "textures/gui/structurize/buildtool/" + fileName);
    }

    private enum StarterEntry {
        SUPPLY_CAMP("supplycamp", "rtcolony.build_drawer.supply_camp"),
        SUPPLY_SHIP("supplyship", "rtcolony.build_drawer.supply_ship");

        private final String type;
        private final String labelKey;

        StarterEntry(String type, String labelKey) {
            this.type = type;
            this.labelKey = labelKey;
        }

        private Component label() {
            return Component.translatable(this.labelKey);
        }

        private String blueprintPath() {
            return "decorations/supplies/" + this.type + ".blueprint";
        }
    }
}
