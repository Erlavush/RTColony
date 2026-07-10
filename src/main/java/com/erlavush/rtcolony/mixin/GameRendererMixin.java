package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 * Orthographic projection adapted from Reign of Nether's OrthoViewMixin.
 * RTColony keeps its own detached camera and cursor raycast implementation.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getProjectionMatrix", at = @At("HEAD"), cancellable = true)
    private void rtcolony$useTrueIsometricProjection(double fov, CallbackInfoReturnable<Matrix4f> ci) {
        if (RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric()) {
            ci.setReturnValue(RtsCameraState.getIsometricProjection(Minecraft.getInstance()));
        }
    }
}
