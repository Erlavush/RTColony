package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft offsets a perspective frustum until it contains a complete 8-block
 * camera cube. That iterative step is not valid for the orthographic RTColony
 * projection and can leave one side of the screen with stale visible sections.
 */
@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
    private void rtcolony$keepOrthographicFrustum(int cubeSize, CallbackInfoReturnable<Frustum> ci) {
        if (RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric()) {
            ci.setReturnValue((Frustum) (Object) this);
        }
    }
}
