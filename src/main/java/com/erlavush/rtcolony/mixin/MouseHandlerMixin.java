package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
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
    private boolean isMiddlePressed;

    @Shadow
    private boolean ignoreFirstMove;

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void rtcolony$preventMouseGrabInRtsMode(CallbackInfo ci) {
        if (RtsModeState.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void rtcolony$panRtsCameraFromMiddleDrag(long window, double mouseX, double mouseY, CallbackInfo ci) {
        if (!RtsModeState.isEnabled() || !RtsCameraState.isActive()) {
            return;
        }

        if (window != this.minecraft.getWindow().getWindow()
                || !this.minecraft.isWindowActive()
                || this.minecraft.screen != null
                || this.ignoreFirstMove
                || !this.isMiddlePressed) {
            return;
        }

        RtsCameraState.panFromScreenDrag(mouseX - this.xpos, mouseY - this.ypos);
    }
}
