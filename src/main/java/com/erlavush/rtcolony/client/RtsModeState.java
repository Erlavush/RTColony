package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import net.minecraft.client.Minecraft;

public final class RtsModeState {
    private static boolean enabled;

    private RtsModeState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void setEnabled(boolean enabled) {
        if (RtsModeState.enabled == enabled) {
            return;
        }

        RtsModeState.enabled = enabled;
        if (enabled) {
            RtsCameraState.activateFromPlayer(Minecraft.getInstance().player);
            Minecraft.getInstance().mouseHandler.releaseMouse();
        } else {
            RtsCameraState.deactivate();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null && minecraft.isWindowActive()) {
                minecraft.mouseHandler.grabMouse();
            }
        }
        RTColony.LOGGER.info("RTS mode {}", enabled ? "enabled" : "disabled");
    }
}
