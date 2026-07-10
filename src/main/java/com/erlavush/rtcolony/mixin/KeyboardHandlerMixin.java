package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsBuildDrawer;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.Minecraft;
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
        Minecraft minecraft = Minecraft.getInstance();
        if (RtsModeState.isEnabled()
                && action == GLFW.GLFW_PRESS
                && key == GLFW.GLFW_KEY_F5
                && minecraft.screen == null) {
            RtsModeState.setEnabled(false);
            ci.cancel();
            return;
        }

        if (!RtsModeState.isEnabled()
                || action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT
                || minecraft.screen != null) {
            return;
        }

        boolean pressed = action == GLFW.GLFW_PRESS;

        if (pressed && key == GLFW.GLFW_KEY_B && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            RtsBuildDrawer.toggle();
            ci.cancel();
            return;
        }

        if (RtsBuildDrawer.isPlacementLocked() && handleLockedPlacementKey(minecraft, key, pressed)) {
            ci.cancel();
            return;
        }

        if (key == GLFW.GLFW_KEY_R && RtsBuildDrawer.rotatePreview()) {
            ci.cancel();
            return;
        }

        if ((key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_PAGE_DOWN)
                && RtsBuildDrawer.adjustPreviewHeight(minecraft, -1)) {
            ci.cancel();
            return;
        }

        if ((key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_PAGE_UP)
                && RtsBuildDrawer.adjustPreviewHeight(minecraft, 1)) {
            ci.cancel();
            return;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE && RtsBuildDrawer.cancelPreview()) {
            ci.cancel();
        }
    }

    private static boolean handleLockedPlacementKey(Minecraft minecraft, int key, boolean pressed) {
        if (key == GLFW.GLFW_KEY_W) {
            return RtsBuildDrawer.moveLockedPreviewRelativeToCamera(0, 1);
        }
        if (key == GLFW.GLFW_KEY_S) {
            return RtsBuildDrawer.moveLockedPreviewRelativeToCamera(0, -1);
        }
        if (key == GLFW.GLFW_KEY_A) {
            return RtsBuildDrawer.moveLockedPreviewRelativeToCamera(1, 0);
        }
        if (key == GLFW.GLFW_KEY_D) {
            return RtsBuildDrawer.moveLockedPreviewRelativeToCamera(-1, 0);
        }
        if (key == GLFW.GLFW_KEY_Q) {
            return RtsBuildDrawer.rotatePreviewLeft();
        }
        if (key == GLFW.GLFW_KEY_R) {
            return RtsBuildDrawer.rotatePreview();
        }
        if (key == GLFW.GLFW_KEY_F) {
            return RtsBuildDrawer.mirrorPreview();
        }
        if (pressed && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            return RtsBuildDrawer.confirmPreview(minecraft);
        }
        return pressed && key == GLFW.GLFW_KEY_ESCAPE && RtsBuildDrawer.cancelPreview();
    }
}
