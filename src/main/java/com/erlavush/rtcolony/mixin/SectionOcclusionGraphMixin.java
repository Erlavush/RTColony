package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A perspective view only rechecks the rendered section list after camera
 * movement. The orthographic projection changes the visible edge sections
 * without that normal perspective movement, so reapply its frustum each frame.
 */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @Inject(method = "consumeFrustumUpdate", at = @At("HEAD"), cancellable = true)
    private void rtcolony$refreshOrthographicFrustum(CallbackInfoReturnable<Boolean> ci) {
        if (RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric()) {
            ci.setReturnValue(true);
        }
    }
}
