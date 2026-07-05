package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsBuildDrawer;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void rtcolony$handleBuildDrawerHotkey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (!RtsModeState.isEnabled()
                || action != GLFW.GLFW_PRESS
                || key != GLFW.GLFW_KEY_B
                || (modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            return;
        }

        RtsBuildDrawer.toggle();
        ci.cancel();
    }
}
