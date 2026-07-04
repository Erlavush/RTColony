package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;

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
        RTColony.LOGGER.info("RTS mode {}", enabled ? "enabled" : "disabled");
    }
}
