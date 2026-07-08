package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class RTColonyConfigScreen extends Screen {
    private static final float EDGE_PAN_SENSITIVITY_MIN = 0.25F;
    private static final float EDGE_PAN_SENSITIVITY_MAX = 3.0F;

    private final Screen parent;

    public RTColonyConfigScreen(Screen parent) {
        super(Component.translatable("rtcolony.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 260;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int y = Math.max(46, this.height / 4 - 24);

        this.addRenderableWidget(Button.builder(edgePanningLabel(), button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    boolean next = !RTColonyClientConfig.get(minecraft).edgePanningEnabled();
                    RTColonyClientConfig.setEdgePanningEnabled(minecraft, next);
                    button.setMessage(edgePanningLabel());
                })
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(new EdgePanningSensitivitySlider(centerX - buttonWidth / 2, y + 24, buttonWidth, buttonHeight));

        this.addRenderableWidget(Button.builder(horizontalOrbitLabel(), button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    boolean next = !RTColonyClientConfig.get(minecraft).invertLockedPlacementOrbitHorizontal();
                    RTColonyClientConfig.setInvertLockedPlacementOrbitHorizontal(minecraft, next);
                    button.setMessage(horizontalOrbitLabel());
                })
                .bounds(centerX - buttonWidth / 2, y + 74, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(verticalOrbitLabel(), button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    boolean next = !RTColonyClientConfig.get(minecraft).invertLockedPlacementOrbitVertical();
                    RTColonyClientConfig.setInvertLockedPlacementOrbitVertical(minecraft, next);
                    button.setMessage(verticalOrbitLabel());
                })
                .bounds(centerX - buttonWidth / 2, y + 98, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.parent))
                .bounds(centerX - 100, this.height - 28, 200, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("rtcolony.config.section.camera"),
                this.width / 2,
                Math.max(30, this.height / 4 - 42),
                0xA0A0A0
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("rtcolony.config.section.locked_placement"),
                this.width / 2,
                Math.max(30, this.height / 4 + 32),
                0xA0A0A0
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static Component horizontalOrbitLabel() {
        return Component.translatable(
                "rtcolony.config.locked_orbit_horizontal",
                orbitValue(RTColonyClientConfig.get(Minecraft.getInstance()).invertLockedPlacementOrbitHorizontal())
        );
    }

    private static Component verticalOrbitLabel() {
        return Component.translatable(
                "rtcolony.config.locked_orbit_vertical",
                orbitValue(RTColonyClientConfig.get(Minecraft.getInstance()).invertLockedPlacementOrbitVertical())
        );
    }

    private static Component orbitValue(boolean inverted) {
        return Component.translatable(inverted ? "rtcolony.config.value.inverted" : "rtcolony.config.value.normal");
    }

    private static Component edgePanningLabel() {
        return Component.translatable(
                "rtcolony.config.edge_panning",
                Component.translatable(RTColonyClientConfig.get(Minecraft.getInstance()).edgePanningEnabled()
                        ? "rtcolony.config.value.enabled"
                        : "rtcolony.config.value.disabled")
        );
    }

    private static Component edgePanningSpeedLabel() {
        return Component.translatable(
                "rtcolony.config.edge_panning_speed",
                Component.literal(Math.round(RTColonyClientConfig.get(Minecraft.getInstance()).edgePanningSensitivity() * 100.0F) + "%")
        );
    }

    private static double sliderValue(float sensitivity) {
        float clamped = Mth.clamp(sensitivity, EDGE_PAN_SENSITIVITY_MIN, EDGE_PAN_SENSITIVITY_MAX);
        return (clamped - EDGE_PAN_SENSITIVITY_MIN) / (EDGE_PAN_SENSITIVITY_MAX - EDGE_PAN_SENSITIVITY_MIN);
    }

    private static float sensitivityValue(double sliderValue) {
        return (float) (EDGE_PAN_SENSITIVITY_MIN + sliderValue * (EDGE_PAN_SENSITIVITY_MAX - EDGE_PAN_SENSITIVITY_MIN));
    }

    private static final class EdgePanningSensitivitySlider extends AbstractSliderButton {
        private EdgePanningSensitivitySlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    edgePanningSpeedLabel(),
                    sliderValue(RTColonyClientConfig.get(Minecraft.getInstance()).edgePanningSensitivity())
            );
        }

        @Override
        protected void updateMessage() {
            this.setMessage(edgePanningSpeedLabel());
        }

        @Override
        protected void applyValue() {
            RTColonyClientConfig.setEdgePanningSensitivity(Minecraft.getInstance(), sensitivityValue(this.value));
        }
    }
}
