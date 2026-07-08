package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsBuildDrawer;
import com.erlavush.rtcolony.client.RTColonyClientConfig;
import com.erlavush.rtcolony.client.RtsModeState;
import com.erlavush.rtcolony.client.RtsTargetingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Shadow
    private boolean isLeftPressed;

    @Shadow
    private boolean isRightPressed;

    @Shadow
    private boolean ignoreFirstMove;

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void rtcolony$preventMouseGrabInRtsMode(CallbackInfo ci) {
        if (RtsModeState.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void rtcolony$moveRtsCameraFromMouseDrag(long window, double mouseX, double mouseY, CallbackInfo ci) {
        if (!RtsModeState.isEnabled() || !RtsCameraState.isActive()) {
            return;
        }

        if (window != this.minecraft.getWindow().getWindow()
                || !this.minecraft.isWindowActive()
                || this.minecraft.screen != null
                || this.ignoreFirstMove) {
            return;
        }

        if (this.isRightPressed) {
            if (RtsBuildDrawer.isPlacementLocked()) {
                RTColonyClientConfig.Config config = RTColonyClientConfig.get(this.minecraft);
                RtsCameraState.orbitLockedPlacementFromScreenDrag(
                        mouseX - this.xpos,
                        mouseY - this.ypos,
                        config.invertLockedPlacementOrbitHorizontal(),
                        config.invertLockedPlacementOrbitVertical()
                );
            } else {
                RtsCameraState.rotateFromScreenDrag(mouseX - this.xpos);
            }
        } else if (this.isLeftPressed) {
            if (!RtsBuildDrawer.isPlacementLocked()) {
                RtsCameraState.panFromScreenDrag(mouseX - this.xpos, mouseY - this.ypos);
            }
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void rtcolony$handleRtsDrawerClick(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!RtsModeState.isEnabled()
                || window != this.minecraft.getWindow().getWindow()
                || this.minecraft.screen != null) {
            return;
        }

        if (RtsBuildDrawer.handleMousePress(this.minecraft, button, action, this.xpos, this.ypos)) {
            ci.cancel();
        }
    }

    @Inject(method = "onPress", at = @At("TAIL"))
    private void rtcolony$selectRtsTarget(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!RtsModeState.isEnabled()
                || window != this.minecraft.getWindow().getWindow()
                || this.minecraft.screen != null
                || button != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || action != GLFW.GLFW_PRESS) {
            return;
        }

        RtsTargetingState.updateHover(this.minecraft);
        RtsTargetingState.selectHovered();
    }
}
