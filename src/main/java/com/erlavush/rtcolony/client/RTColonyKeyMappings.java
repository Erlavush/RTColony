package com.erlavush.rtcolony.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class RTColonyKeyMappings {
    public static final String CATEGORY = "key.categories.rtcolony";

    public static final KeyMapping TOGGLE_RTS_MODE = new KeyMapping(
            "key.rtcolony.toggle_rts_mode",
            InputConstants.KEY_F4,
            CATEGORY
    );
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.rtcolony.open_config",
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    private RTColonyKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RTS_MODE);
        event.register(OPEN_CONFIG);
    }
}
