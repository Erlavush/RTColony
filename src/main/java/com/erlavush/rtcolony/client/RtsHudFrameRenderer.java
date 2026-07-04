package com.erlavush.rtcolony.client;

import net.minecraft.client.gui.GuiGraphics;

final class RtsHudFrameRenderer {
    private RtsHudFrameRenderer() {
    }

    static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xE0000000);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xC0202020);

        guiGraphics.fill(x, y, x + width, y + 1, 0xFFE0E0E0);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFE0E0E0);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF7A7A7A);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF7A7A7A);

        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF151515);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF151515);
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF3A3A3A);
        guiGraphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF3A3A3A);
    }

    static void drawInset(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0A0A0A);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xB0303030);

        guiGraphics.fill(x, y, x + width, y + 1, 0xFF151515);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF151515);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF3A3A3A);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF3A3A3A);

        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFFE0E0E0);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFFE0E0E0);
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF7A7A7A);
        guiGraphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF7A7A7A);
    }
}
