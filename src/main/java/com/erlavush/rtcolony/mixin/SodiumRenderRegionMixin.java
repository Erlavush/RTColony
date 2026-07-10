package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium caches draw commands after applying its directional face mask. Clear
 * each region's commands once when entering or leaving True Isometric so a mask
 * produced by the previous projection cannot survive the mode switch.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion", remap = false)
public abstract class SodiumRenderRegionMixin {
    @Shadow(remap = false)
    public abstract void clearAllCachedBatches();

    @Unique
    private boolean rtcolony$cachedForTrueIsometric;

    @Inject(method = "getCachedBatch", at = @At("HEAD"), remap = false)
    private void rtcolony$refreshFaceCullingCommands(CallbackInfoReturnable<?> ci) {
        boolean trueIsometric = RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric();
        if (trueIsometric != this.rtcolony$cachedForTrueIsometric) {
            this.clearAllCachedBatches();
            this.rtcolony$cachedForTrueIsometric = trueIsometric;
        }
    }
}
