package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.minecolonies.api.items.ModItems;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.network.messages.BuildToolPlacementMessage;
import com.ldtteam.structurize.placement.handlers.placement.PlacementError;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.storage.rendering.RenderingCache;
import com.ldtteam.structurize.storage.rendering.types.BlueprintPreviewData;
import com.minecolonies.core.items.ItemSupplyCampDeployer;
import com.minecolonies.core.items.ItemSupplyChestDeployer;
import com.minecolonies.core.placementhandlers.main.SuppliesHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Rotation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class RtsBuildDrawer {
    private static final String PREVIEW_KEY = "supplies";
    private static final int PAPER_TEXTURE_WIDTH = 190;
    private static final int PAPER_TEXTURE_HEIGHT = 244;
    private static final int PAPER_SHORT_TEXTURE_HEIGHT = 130;
    private static final int PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 314;
    private static final int ENTRY_WIDTH = 86;
    private static final int ENTRY_HEIGHT = 17;
    private static final int PLACEMENT_FOLLOWING_PANEL_WIDTH = 228;
    private static final int PLACEMENT_FOLLOWING_PANEL_HEIGHT = 46;
    private static final int PLACEMENT_LOCKED_PANEL_WIDTH = 126;
    private static final int PLACEMENT_LOCKED_PANEL_HEIGHT = 176;
    private static final int PLACEMENT_BUTTON_SIZE = 24;
    private static final int PLACEMENT_BUTTON_ICON_SIZE = 20;
    private static final int PLACEMENT_BUTTON_ICON_SOURCE_SIZE = 32;
    private static final int PLACEMENT_BUTTON_GAP = 4;
    private static final long PLACEMENT_VALIDATION_INTERVAL_MS = 250L;

    private static final ResourceLocation PAPER = mineColoniesTexture("builder_paper.png");
    private static final ResourceLocation PAPER_SHORT = mineColoniesTexture("builder_paper_short.png");
    private static final ResourceLocation TAB_BUTTON = mineColoniesTexture("builder_button_medium.png");
    private static final ResourceLocation ENTRY_BUTTON = structurizeTexture("button_blueprint.png");
    private static final ResourceLocation ENTRY_BUTTON_SELECTED = structurizeTexture("button_blueprint_selected.png");
    private static final ResourceLocation CONFIRM_ICON = structurizeTexture("confirm.png");
    private static final ResourceLocation CANCEL_ICON = structurizeTexture("cancel.png");
    private static final ResourceLocation MIRROR_ICON = structurizeTexture("mirror.png");
    private static final ResourceLocation ROTATE_LEFT_ICON = structurizeTexture("rotateleft.png");
    private static final ResourceLocation ROTATE_RIGHT_ICON = structurizeTexture("rotateright.png");
    private static final ResourceLocation LEFT_ICON = structurizeTexture("left.png");
    private static final ResourceLocation RIGHT_ICON = structurizeTexture("right.png");
    private static final ResourceLocation UP_ICON = structurizeTexture("up.png");
    private static final ResourceLocation DOWN_ICON = structurizeTexture("down.png");
    private static final ResourceLocation PLUS_ICON = structurizeTexture("plus.png");
    private static final ResourceLocation MINUS_ICON = structurizeTexture("minus.png");

    private static boolean open;
    private static StarterEntry selectedEntry = StarterEntry.SUPPLY_CAMP;
    private static boolean previewActive;
    private static StructurePackMeta selectedStructurePack;
    private static PlacementMode placementMode = PlacementMode.IDLE;
    private static PlacementState placementState = PlacementState.IDLE;
    private static int placementErrorCount;
    private static long nextPlacementValidationMillis;
    private static BlockPos lastValidatedPos;
    private static RotationMirror lastValidatedRotationMirror;
    private static int previewYOffset;

    private RtsBuildDrawer() {
    }

    public static void toggle() {
        open = !open;
    }

    static void close() {
        open = false;
    }

    static void tick(Minecraft minecraft) {
        if (previewActive) {
            if (placementMode == PlacementMode.FOLLOWING_CURSOR) {
                updatePreviewPosition(minecraft);
            }
            updatePlacementValidation(minecraft, false);
        }
    }

    static void clearPreview() {
        previewActive = false;
        selectedStructurePack = null;
        placementMode = PlacementMode.IDLE;
        placementState = PlacementState.IDLE;
        placementErrorCount = 0;
        previewYOffset = 0;
        invalidatePlacementValidation();
        RenderingCache.removeBlueprint(PREVIEW_KEY);
    }

    public static boolean cancelPreview() {
        if (!previewActive) {
            return false;
        }

        if (placementMode == PlacementMode.LOCKED_ADJUSTING) {
            RtsCameraState.returnToRtsView();
        }

        clearPreview();
        open = true;
        return true;
    }

    public static boolean confirmPreview(Minecraft minecraft) {
        return confirmPlacement(minecraft);
    }

    public static boolean isPlacementLocked() {
        return previewActive && placementMode == PlacementMode.LOCKED_ADJUSTING;
    }

    public static boolean rotatePreview() {
        return rotatePreview(Rotation.CLOCKWISE_90);
    }

    public static boolean rotatePreviewLeft() {
        return rotatePreview(Rotation.COUNTERCLOCKWISE_90);
    }

    private static boolean rotatePreview(Rotation rotation) {
        if (!previewActive) {
            return false;
        }

        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData != null && previewData.getBlueprint() != null) {
            Vec3 previousCenter = placementMode == PlacementMode.LOCKED_ADJUSTING ? previewCenter(previewData) : null;
            previewData.rotate(rotation);
            RenderingCache.queue(PREVIEW_KEY, previewData);
            shiftCameraForPreviewCenterChange(previousCenter, previewData);
            invalidatePlacementValidation();
        }
        return true;
    }

    public static boolean mirrorPreview() {
        if (!previewActive) {
            return false;
        }

        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData != null && previewData.getBlueprint() != null) {
            Vec3 previousCenter = placementMode == PlacementMode.LOCKED_ADJUSTING ? previewCenter(previewData) : null;
            previewData.mirror();
            RenderingCache.queue(PREVIEW_KEY, previewData);
            shiftCameraForPreviewCenterChange(previousCenter, previewData);
            invalidatePlacementValidation();
        }
        return true;
    }

    public static boolean adjustPreviewHeight(Minecraft minecraft, int deltaY) {
        if (!previewActive) {
            return false;
        }

        int previousYOffset = previewYOffset;
        previewYOffset = Math.max(-32, Math.min(64, previewYOffset + deltaY));
        int appliedDeltaY = previewYOffset - previousYOffset;
        if (appliedDeltaY == 0) {
            return true;
        }

        if (placementMode == PlacementMode.LOCKED_ADJUSTING) {
            movePreview(0, appliedDeltaY, 0);
        } else {
            updatePreviewPosition(minecraft);
        }
        invalidatePlacementValidation();
        return true;
    }

    public static boolean moveLockedPreviewRelativeToCamera(int leftImpulse, int forwardImpulse) {
        if (placementMode != PlacementMode.LOCKED_ADJUSTING || leftImpulse == 0 && forwardImpulse == 0) {
            return false;
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, RtsCameraState.getTargetYaw()).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 movement = forward.scale(forwardImpulse).add(left.scale(leftImpulse));
        int deltaX = blockStep(movement.x);
        int deltaZ = blockStep(movement.z);
        if (deltaX == 0 && deltaZ == 0) {
            return true;
        }
        return movePreview(deltaX, 0, deltaZ);
    }

    static boolean isMouseOver(Minecraft minecraft) {
        if (!RtsModeState.isEnabled() || minecraft.screen != null) {
            return false;
        }

        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);
        if (previewActive && isInsidePlacementPanel(minecraft, mouseX, mouseY)) {
            return true;
        }

        if (open) {
            return isInside(mouseX, mouseY, panelX(), panelY(minecraft), PANEL_WIDTH, PANEL_HEIGHT);
        }
        RtsBuildDrawerConfig.Config config = RtsBuildDrawerConfig.get(minecraft);
        return isInside(mouseX, mouseY, collapsedX(config), collapsedY(minecraft, config), config.collapsedTabWidth, config.collapsedTabHeight);
    }

    public static boolean handleMousePress(Minecraft minecraft, int button, int action, double rawMouseX, double rawMouseY) {
        if (!RtsModeState.isEnabled()
                || minecraft.screen != null
                || action != GLFW.GLFW_PRESS) {
            return false;
        }

        int mouseX = scaledMouseX(minecraft, rawMouseX);
        int mouseY = scaledMouseY(minecraft, rawMouseY);

        if (previewActive && placementMode == PlacementMode.LOCKED_ADJUSTING) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (isInsidePlacementPanel(minecraft, mouseX, mouseY)) {
                    return handlePlacementPanelClick(minecraft, mouseX, mouseY);
                }
                return true;
            }
            return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && previewActive) {
            return lockPreview(minecraft);
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (!open) {
            RtsBuildDrawerConfig.Config config = RtsBuildDrawerConfig.get(minecraft);
            if (isInside(mouseX, mouseY, collapsedX(config), collapsedY(minecraft, config), config.collapsedTabWidth, config.collapsedTabHeight)) {
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

        renderPlacementHud(minecraft, guiGraphics);
    }

    private static void renderOpen(Minecraft minecraft, GuiGraphics guiGraphics) {
        int x = panelX();
        int y = panelY(minecraft);
        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);
        Font font = minecraft.font;
        RtsBuildDrawerConfig.Config config = RtsBuildDrawerConfig.get(minecraft);

        drawScaledTexture(guiGraphics, PAPER, x, y, PAPER_TEXTURE_WIDTH, PAPER_TEXTURE_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        drawArrowText(guiGraphics, font, config.openArrowText, x + config.openArrowOffsetX, y + config.openArrowOffsetY, config.openArrowScale, config);

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
                previewStateLabel(),
                x + 18,
                y + 167,
                0xFF7F6041,
                false
        );
    }

    private static Component previewStateLabel() {
        if (!previewActive) {
            return Component.translatable("rtcolony.build_drawer.preview_inactive");
        }

        return Component.translatable(placementMode == PlacementMode.LOCKED_ADJUSTING
                ? "rtcolony.build_drawer.preview_locked"
                : "rtcolony.build_drawer.preview_active");
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
        RtsBuildDrawerConfig.Config config = RtsBuildDrawerConfig.get(minecraft);
        int x = collapsedX(config);
        int y = collapsedY(minecraft, config);
        int height = config.collapsedTabHeight;
        int width = config.collapsedTabWidth;
        int center = height / 2;
        for (int row = 0; row < height; row++) {
            int distanceFromCenter = Math.abs(row - center);
            int taper = distanceFromCenter * config.collapsedTabTaper / center;
            int rowWidth = width - taper;
            guiGraphics.fill(x, y + row, x + rowWidth, y + row + 1, config.borderColor());
        }
        int firstPaperRow = config.collapsedTabPaperInsetTop;
        int lastPaperRow = height - config.collapsedTabPaperInsetBottom;
        int paperRows = Math.max(1, lastPaperRow - firstPaperRow);
        for (int row = firstPaperRow; row < lastPaperRow; row++) {
            int distanceFromCenter = Math.abs(row - center);
            int taper = distanceFromCenter * config.collapsedTabTaper / center;
            int rowWidth = width - taper;
            int paperWidth = Math.max(0, rowWidth - config.collapsedTabPaperInsetLeft - config.collapsedTabPaperInsetRight);
            float sourceY = config.collapsedTabPaperSourceY + (float) (row - firstPaperRow) * config.collapsedTabPaperSourceHeight / paperRows;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, PAPER_SHORT);
            guiGraphics.blit(
                    PAPER_SHORT,
                    x + config.collapsedTabPaperOffsetX + config.collapsedTabPaperInsetLeft,
                    y + config.collapsedTabPaperOffsetY + row,
                    0,
                    config.collapsedTabPaperSourceX,
                    sourceY,
                    paperWidth,
                    1,
                    PAPER_TEXTURE_WIDTH,
                    PAPER_SHORT_TEXTURE_HEIGHT
            );
        }
        for (int row = 4; row < height / 2; row++) {
            int distanceFromCenter = Math.abs(row - center);
            int taper = distanceFromCenter * config.collapsedTabTaper / center;
            int rowWidth = width - taper;
            int highlightStartX = x + config.collapsedTabHighlightInsetLeft;
            int highlightEndX = x + Math.max(config.collapsedTabHighlightInsetLeft, rowWidth - config.collapsedTabHighlightInsetRight);
            guiGraphics.fill(highlightStartX, y + row, highlightEndX, y + row + 1, config.highlightColor());
        }
        int arrowWidth = Math.round(minecraft.font.width(config.collapsedArrowText) * config.collapsedArrowScale);
        int arrowHeight = Math.round(minecraft.font.lineHeight * config.collapsedArrowScale);
        int arrowX = x + (width - arrowWidth) / 2 + config.collapsedArrowOffsetX;
        int arrowY = y + (height - arrowHeight) / 2 + config.collapsedArrowOffsetY;
        drawArrowText(guiGraphics, minecraft.font, config.collapsedArrowText, arrowX, arrowY, config.collapsedArrowScale, config);
    }

    private static void drawArrowText(GuiGraphics guiGraphics, Font font, String arrow, int x, int y, float scale, RtsBuildDrawerConfig.Config config) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, arrow, config.arrowShadowOffsetX, config.arrowShadowOffsetY, config.arrowShadowColor(), false);
        guiGraphics.drawString(font, arrow, 0, 0, config.arrowColor(), false);
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

    private static void drawScaledIcon(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        float scale = (float) size / PLACEMENT_BUTTON_ICON_SOURCE_SIZE;
        poseStack.scale(scale, scale, 1.0F);
        drawTexture(
                guiGraphics,
                texture,
                0,
                0,
                PLACEMENT_BUTTON_ICON_SOURCE_SIZE,
                PLACEMENT_BUTTON_ICON_SOURCE_SIZE,
                PLACEMENT_BUTTON_ICON_SOURCE_SIZE,
                PLACEMENT_BUTTON_ICON_SOURCE_SIZE
        );
        poseStack.popPose();
    }

    private static void selectEntry(Minecraft minecraft, StarterEntry entry) {
        selectedEntry = entry;
        open = false;
        loadPreview(minecraft);
    }

    private static void loadPreview(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }

        try {
            clearPreview();
            StructurePacks.ensureSelectedPack();
            selectedStructurePack = StructurePacks.getSelectedPack();
            if (selectedStructurePack == null) {
                previewActive = false;
                RenderingCache.removeBlueprint(PREVIEW_KEY);
                RTColony.LOGGER.warn("Unable to load {} preview: Structurize has no selected structure pack", selectedEntry.type);
                return;
            }

            BlueprintPreviewData previewData = RenderingCache.getOrCreateBlueprintPreviewData(PREVIEW_KEY);
            previewData.setPos(previewPosition(minecraft));
            previewData.setBlueprint(null);
            previewData.setBlueprintFuture(StructurePacks.getBlueprintFuture(
                    selectedStructurePack.getName(),
                    selectedEntry.blueprintPath(),
                    minecraft.level.registryAccess()
            ));
            previewData.setRenderBlocksNice(true);
            previewData.setOverridePreviewTransparency(0.55F);
            RenderingCache.queue(PREVIEW_KEY, previewData);
            previewActive = true;
            placementMode = PlacementMode.FOLLOWING_CURSOR;
            placementState = PlacementState.LOADING;
            invalidatePlacementValidation();
        } catch (RuntimeException exception) {
            clearPreview();
            RTColony.LOGGER.warn("Unable to load {} Structurize preview", selectedEntry.type, exception);
        }
    }

    private static void updatePreviewPosition(Minecraft minecraft) {
        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData != null) {
            previewData.setPos(previewPosition(minecraft));
        }
    }

    private static boolean lockPreview(Minecraft minecraft) {
        if (!previewActive || placementMode != PlacementMode.FOLLOWING_CURSOR) {
            return false;
        }

        updatePreviewPosition(minecraft);
        placementMode = PlacementMode.LOCKED_ADJUSTING;
        focusCameraOnPreview();
        invalidatePlacementValidation();
        updatePlacementValidation(minecraft, true);
        return true;
    }

    private static boolean movePreview(int deltaX, int deltaY, int deltaZ) {
        if (!previewActive) {
            return false;
        }

        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData == null || previewData.getPos() == null) {
            return true;
        }

        previewData.setPos(previewData.getPos().offset(deltaX, deltaY, deltaZ));
        RenderingCache.queue(PREVIEW_KEY, previewData);
        RtsCameraState.shiftFocus(deltaX, deltaY, deltaZ);
        invalidatePlacementValidation();
        return true;
    }

    private static int blockStep(double value) {
        if (Math.abs(value) < 0.5D) {
            return 0;
        }
        return value > 0.0D ? 1 : -1;
    }

    private static void focusCameraOnPreview() {
        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData != null) {
            RtsCameraState.focusOn(previewCenter(previewData));
        }
    }

    private static void shiftCameraForPreviewCenterChange(Vec3 previousCenter, BlueprintPreviewData previewData) {
        if (previousCenter == null || placementMode != PlacementMode.LOCKED_ADJUSTING) {
            return;
        }

        Vec3 newCenter = previewCenter(previewData);
        RtsCameraState.shiftFocus(newCenter.x - previousCenter.x, newCenter.y - previousCenter.y, newCenter.z - previousCenter.z);
    }

    private static Vec3 previewCenter(BlueprintPreviewData previewData) {
        BlockPos pos = previewData.getPos();
        if (pos == null) {
            return Vec3.ZERO;
        }

        Blueprint blueprint = previewData.getBlueprint();
        if (blueprint == null) {
            return Vec3.atCenterOf(pos);
        }

        BlockPos primaryOffset = blueprint.getPrimaryBlockOffset();
        if (primaryOffset == null) {
            primaryOffset = BlockPos.ZERO;
        }

        Vec3 localCenter = new Vec3(
                (blueprint.getSizeX() - 1) / 2.0D,
                (blueprint.getSizeY() - 1) / 2.0D,
                (blueprint.getSizeZ() - 1) / 2.0D
        );
        Vec3 relativeCenter = localCenter.subtract(Vec3.atLowerCornerOf(primaryOffset));
        Vec3 transformedCenter = previewData.getRotationMirror().applyToPos(relativeCenter);
        return Vec3.atCenterOf(pos).add(transformedCenter);
    }

    private static boolean confirmPlacement(Minecraft minecraft) {
        if (!previewActive) {
            return false;
        }

        if (placementMode != PlacementMode.LOCKED_ADJUSTING) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("rtcolony.placement.lock_first"), true);
            }
            return true;
        }

        updatePlacementValidation(minecraft, true);
        if (placementState != PlacementState.VALID) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(placementStatusText()), true);
            }
            return true;
        }

        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData == null || previewData.getBlueprint() == null || selectedStructurePack == null) {
            placementState = PlacementState.INVALID;
            return true;
        }

        try {
            new BuildToolPlacementMessage(
                    BuildToolPlacementMessage.HandlerType.Survival,
                    SuppliesHandler.ID,
                    selectedStructurePack.getName(),
                    blueprintPath(previewData.getBlueprint()),
                    previewData.getPos(),
                    previewData.getRotationMirror()
            ).sendToServer();

            RtsCameraState.returnToRtsView();
            clearPreview();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("rtcolony.placement.sent"), true);
            }
        } catch (RuntimeException exception) {
            placementState = PlacementState.INVALID;
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("rtcolony.placement.failed"), true);
            }
            RTColony.LOGGER.warn("Unable to send {} placement request", selectedEntry.type, exception);
        }
        return true;
    }

    private static void updatePlacementValidation(Minecraft minecraft, boolean force) {
        if (!previewActive) {
            placementState = PlacementState.IDLE;
            return;
        }

        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        if (previewData == null || previewData.getBlueprint() == null) {
            placementState = PlacementState.LOADING;
            return;
        }

        BlockPos pos = previewData.getPos();
        if (minecraft.level == null || minecraft.player == null || selectedStructurePack == null || pos == null) {
            placementState = PlacementState.INVALID;
            placementErrorCount = 0;
            return;
        }

        if (!hasRequiredSupplyItem(minecraft)) {
            placementState = PlacementState.MISSING_ITEM;
            placementErrorCount = 0;
            return;
        }

        RotationMirror rotationMirror = previewData.getRotationMirror();
        long now = System.currentTimeMillis();
        if (!force
                && now < nextPlacementValidationMillis
                && pos.equals(lastValidatedPos)
                && rotationMirror == lastValidatedRotationMirror) {
            return;
        }

        nextPlacementValidationMillis = now + PLACEMENT_VALIDATION_INTERVAL_MS;
        lastValidatedPos = pos;
        lastValidatedRotationMirror = rotationMirror;

        List<PlacementError> placementErrors = new ArrayList<>();
        try {
            boolean valid = switch (selectedEntry) {
                case SUPPLY_CAMP -> ItemSupplyCampDeployer.canCampBePlaced(
                        minecraft.level,
                        pos,
                        placementErrors,
                        minecraft.player
                );
                case SUPPLY_SHIP -> ItemSupplyChestDeployer.canShipBePlaced(
                        minecraft.level,
                        pos,
                        previewData.getBlueprint(),
                        placementErrors,
                        minecraft.player
                );
            };

            placementState = valid ? PlacementState.VALID : PlacementState.INVALID;
            placementErrorCount = placementErrors.size();
        } catch (RuntimeException exception) {
            placementState = PlacementState.INVALID;
            placementErrorCount = 0;
            RTColony.LOGGER.debug("Unable to validate {} placement preview", selectedEntry.type, exception);
        }
    }

    private static void invalidatePlacementValidation() {
        nextPlacementValidationMillis = 0L;
        lastValidatedPos = null;
        lastValidatedRotationMirror = null;
    }

    private static boolean hasRequiredSupplyItem(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.getAbilities().instabuild) {
            return true;
        }

        Item requiredItem = selectedEntry.requiredItem();
        if (requiredItem == null) {
            return true;
        }

        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.is(requiredItem)) {
                return true;
            }
        }
        for (ItemStack stack : minecraft.player.getInventory().offhand) {
            if (stack.is(requiredItem)) {
                return true;
            }
        }
        return false;
    }

    private static String blueprintPath(Blueprint blueprint) {
        if (selectedStructurePack == null || blueprint.getFilePath() == null || blueprint.getFileName() == null) {
            return selectedEntry.blueprintPath();
        }

        try {
            return selectedStructurePack.getSubPath(blueprint.getFilePath().resolve(blueprint.getFileName() + ".blueprint"));
        } catch (RuntimeException exception) {
            return selectedEntry.blueprintPath();
        }
    }

    private static void renderPlacementHud(Minecraft minecraft, GuiGraphics guiGraphics) {
        if (!previewActive) {
            return;
        }

        updatePlacementValidation(minecraft, false);

        Font font = minecraft.font;
        int x = placementPanelX(minecraft);
        int y = placementPanelY(minecraft);
        int width = placementPanelWidth();
        int height = placementPanelHeight();

        renderPlacementPanel(guiGraphics, x, y, width, height);

        String title = trim(font, selectedEntry.label().getString(), width - 16);
        String status = trim(font, placementStatusText(), width - 16);
        guiGraphics.drawString(font, title, x + 8, y + 7, 0xFFFFF2D0, false);
        guiGraphics.drawString(font, status, x + 8, y + 19, placementStatusColor(), false);

        if (placementMode != PlacementMode.LOCKED_ADJUSTING) {
            String controls = Component.translatable("rtcolony.placement.following_controls").getString();
            guiGraphics.drawString(font, trim(font, controls, width - 16), x + 8, y + 31, 0xFFC9AA7F, false);
            return;
        }

        String rotation = "Rot " + rotationDegrees() + "  Y " + signed(previewYOffset);
        guiGraphics.drawString(font, rotation, x + 8, y + 31, 0xFFC9AA7F, false);

        int mouseX = scaledMouseX(minecraft);
        int mouseY = scaledMouseY(minecraft);

        renderToolButtonAt(minecraft, guiGraphics, ROTATE_LEFT_ICON, 0, 0, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, UP_ICON, 1, 0, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, ROTATE_RIGHT_ICON, 2, 0, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, LEFT_ICON, 0, 1, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, DOWN_ICON, 1, 1, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, RIGHT_ICON, 2, 1, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, MINUS_ICON, 0, 2, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, MIRROR_ICON, 1, 2, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, PLUS_ICON, 2, 2, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, CANCEL_ICON, 0, 3, mouseX, mouseY, true);
        renderToolButtonAt(minecraft, guiGraphics, CONFIRM_ICON, 2, 3, mouseX, mouseY, placementState == PlacementState.VALID);
    }

    private static void renderPlacementPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xB0181410);
        guiGraphics.fill(x, y, x + width, y + 1, 0xAAFFF2C8);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xAA3C2818);
        guiGraphics.fill(x, y, x + 1, y + height, 0xAAFFF2C8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xAA3C2818);
    }

    private static void renderToolButtonAt(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            ResourceLocation icon,
            int column,
            int row,
            int mouseX,
            int mouseY,
            boolean enabled
    ) {
        renderToolButton(guiGraphics, icon, toolButtonX(minecraft, column), toolButtonY(minecraft, row), mouseX, mouseY, enabled);
    }

    private static void renderToolButton(
            GuiGraphics guiGraphics,
            ResourceLocation icon,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean enabled
    ) {
        boolean hovered = enabled && isInside(mouseX, mouseY, x, y, PLACEMENT_BUTTON_SIZE, PLACEMENT_BUTTON_SIZE);
        guiGraphics.fill(x - 1, y - 1, x + PLACEMENT_BUTTON_SIZE + 1, y + PLACEMENT_BUTTON_SIZE + 1, hovered ? 0x99FFF2C8 : 0x66442E1D);
        guiGraphics.fill(x, y, x + PLACEMENT_BUTTON_SIZE, y + PLACEMENT_BUTTON_SIZE, enabled ? 0x333C2818 : 0x773C2818);
        drawScaledIcon(
                guiGraphics,
                icon,
                x + (PLACEMENT_BUTTON_SIZE - PLACEMENT_BUTTON_ICON_SIZE) / 2,
                y + (PLACEMENT_BUTTON_SIZE - PLACEMENT_BUTTON_ICON_SIZE) / 2,
                PLACEMENT_BUTTON_ICON_SIZE
        );
    }

    private static boolean handlePlacementPanelClick(Minecraft minecraft, int mouseX, int mouseY) {
        if (!isInsidePlacementPanel(minecraft, mouseX, mouseY)) {
            return false;
        }

        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 0, 0)) {
            return rotatePreviewLeft();
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 1, 0)) {
            return movePreview(0, 0, -1);
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 2, 0)) {
            return rotatePreview();
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 0, 1)) {
            return movePreview(-1, 0, 0);
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 1, 1)) {
            return movePreview(0, 0, 1);
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 2, 1)) {
            return movePreview(1, 0, 0);
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 0, 2)) {
            return adjustPreviewHeight(minecraft, -1);
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 1, 2)) {
            return mirrorPreview();
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 2, 2)) {
            return adjustPreviewHeight(minecraft, 1);
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 0, 3)) {
            return cancelPreview();
        }
        if (isInsideToolButtonAt(minecraft, mouseX, mouseY, 2, 3)) {
            return confirmPlacement(minecraft);
        }

        return true;
    }

    private static boolean isInsidePlacementPanel(Minecraft minecraft, int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, placementPanelX(minecraft), placementPanelY(minecraft), placementPanelWidth(), placementPanelHeight());
    }

    private static boolean isInsideToolButtonAt(Minecraft minecraft, int mouseX, int mouseY, int column, int row) {
        return isInside(mouseX, mouseY, toolButtonX(minecraft, column), toolButtonY(minecraft, row), PLACEMENT_BUTTON_SIZE, PLACEMENT_BUTTON_SIZE);
    }

    private static int buttonStep() {
        return PLACEMENT_BUTTON_SIZE + PLACEMENT_BUTTON_GAP;
    }

    private static int placementPanelWidth() {
        return placementMode == PlacementMode.LOCKED_ADJUSTING ? PLACEMENT_LOCKED_PANEL_WIDTH : PLACEMENT_FOLLOWING_PANEL_WIDTH;
    }

    private static int placementPanelHeight() {
        return placementMode == PlacementMode.LOCKED_ADJUSTING ? PLACEMENT_LOCKED_PANEL_HEIGHT : PLACEMENT_FOLLOWING_PANEL_HEIGHT;
    }

    private static int placementPanelX(Minecraft minecraft) {
        return Math.max(8, minecraft.getWindow().getGuiScaledWidth() - placementPanelWidth() - 10);
    }

    private static int placementPanelY(Minecraft minecraft) {
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int panelHeight = placementPanelHeight();
        int centered = (screenHeight - panelHeight) / 2;
        int maxY = Math.max(8, screenHeight - panelHeight - 8);
        return Math.max(8, Math.min(centered, maxY));
    }

    private static int toolButtonX(Minecraft minecraft, int column) {
        int gridWidth = PLACEMENT_BUTTON_SIZE * 3 + PLACEMENT_BUTTON_GAP * 2;
        return placementPanelX(minecraft) + (PLACEMENT_LOCKED_PANEL_WIDTH - gridWidth) / 2 + column * buttonStep();
    }

    private static int toolButtonY(Minecraft minecraft, int row) {
        return placementPanelY(minecraft) + 58 + row * buttonStep();
    }

    private static String placementStatusText() {
        return switch (placementState) {
            case IDLE, LOADING -> Component.translatable("rtcolony.placement.loading").getString();
            case VALID -> Component.translatable("rtcolony.placement.valid").getString();
            case MISSING_ITEM -> Component.translatable("rtcolony.placement.missing_item", selectedEntry.label()).getString();
            case INVALID -> placementErrorCount > 0
                    ? Component.translatable("rtcolony.placement.invalid_with_count", placementErrorCount).getString()
                    : Component.translatable("rtcolony.placement.invalid").getString();
        };
    }

    private static int placementStatusColor() {
        return switch (placementState) {
            case VALID -> 0xFF63E663;
            case INVALID, MISSING_ITEM -> 0xFFFF6666;
            case IDLE, LOADING -> 0xFFE0D2B2;
        };
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static int rotationDegrees() {
        BlueprintPreviewData previewData = RenderingCache.getBlueprintPreviewData(PREVIEW_KEY);
        Rotation rotation = previewData == null ? Rotation.NONE : previewData.getRotationMirror().rotation();
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 90;
            case CLOCKWISE_180 -> 180;
            case COUNTERCLOCKWISE_90 -> 270;
        };
    }

    private static BlockPos previewPosition(Minecraft minecraft) {
        HitResult hoverHit = RtsTargetingState.getHoverHit();
        BlockPos basePos;
        if (hoverHit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            basePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else if (minecraft.player != null) {
            basePos = minecraft.player.blockPosition();
        } else {
            basePos = BlockPos.ZERO;
        }

        if (selectedEntry == StarterEntry.SUPPLY_SHIP && minecraft.level != null) {
            basePos = snapToWaterSurface(minecraft, basePos);
        }
        return basePos.above(previewYOffset);
    }

    private static BlockPos snapToWaterSurface(Minecraft minecraft, BlockPos basePos) {
        if (minecraft.level == null) {
            return basePos;
        }

        BlockPos waterPos = basePos;
        if (minecraft.level.getFluidState(waterPos).isEmpty()) {
            for (int i = 0; i < 16 && waterPos.getY() < minecraft.level.getMaxBuildHeight() - 1; i++) {
                waterPos = waterPos.above();
                if (!minecraft.level.getFluidState(waterPos).isEmpty()) {
                    break;
                }
            }
        }

        if (minecraft.level.getFluidState(waterPos).isEmpty()) {
            return basePos;
        }

        while (waterPos.getY() < minecraft.level.getMaxBuildHeight() - 1
                && !minecraft.level.getFluidState(waterPos.above()).isEmpty()) {
            waterPos = waterPos.above();
        }
        return waterPos;
    }

    private static int panelX() {
        return 4;
    }

    private static int panelY(Minecraft minecraft) {
        return Math.max(26, (minecraft.getWindow().getGuiScaledHeight() - PANEL_HEIGHT) / 2);
    }

    private static int collapsedX(RtsBuildDrawerConfig.Config config) {
        return config.collapsedTabX;
    }

    private static int collapsedY(Minecraft minecraft, RtsBuildDrawerConfig.Config config) {
        return Math.max(config.collapsedTabTopMargin, (minecraft.getWindow().getGuiScaledHeight() - config.collapsedTabHeight) / 2);
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

    private static String trim(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }

        String ellipsis = "...";
        int bodyWidth = Math.max(0, width - font.width(ellipsis));
        return font.plainSubstrByWidth(text, bodyWidth) + ellipsis;
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

        private Item requiredItem() {
            return switch (this) {
                case SUPPLY_CAMP -> ModItems.supplyCamp;
                case SUPPLY_SHIP -> ModItems.supplyChest;
            };
        }
    }

    private enum PlacementMode {
        IDLE,
        FOLLOWING_CURSOR,
        LOCKED_ADJUSTING
    }

    private enum PlacementState {
        IDLE,
        LOADING,
        VALID,
        MISSING_ITEM,
        INVALID
    }
}
