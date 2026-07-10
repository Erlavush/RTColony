package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium owns terrain selection in the development client. Its directional
 * smart-culling graph assumes a perspective camera, so it can reject foreground
 * sections when RTColony switches to an orthographic isometric projection.
 *
 * This optional compatibility mixin keeps Sodium's ordinary range/frustum
 * culling, but turns off only its directional occlusion traversal in True
 * Isometric mode. Perspective RTS and vanilla views retain Sodium's defaults.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public abstract class SodiumRenderSectionManagerMixin {
    @Inject(method = "shouldUseOcclusionCulling", at = @At("HEAD"), cancellable = true, remap = false)
    private void rtcolony$disablePerspectiveOcclusionInTrueIsometric(
            Camera camera,
            boolean spectator,
            CallbackInfoReturnable<Boolean> ci
    ) {
        if (RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric()) {
            ci.setReturnValue(false);
        }
    }
}
