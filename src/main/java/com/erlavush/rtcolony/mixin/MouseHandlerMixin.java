package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void rtcolony$preventMouseGrabInRtsMode(CallbackInfo ci) {
        if (RtsModeState.isEnabled()) {
            ci.cancel();
        }
    }
}
