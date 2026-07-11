package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsFocusLensShader;
import com.erlavush.rtcolony.client.RtsModeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium normally chooses which directional terrain meshes to draw from the
 * camera point's position relative to each chunk section. An orthographic view
 * has parallel view rays, so sections at the edges of RTColony's viewport can
 * cross that camera point and lose faces which are still facing the view.
 *
 * Returning every available face bit keeps Sodium's compiled terrain meshes
 * intact in True Isometric mode. Sodium immediately intersects this value with
 * the section's real mesh mask, so no nonexistent mesh slices are submitted.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
public abstract class SodiumDefaultChunkRendererMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;iterator(Z)Ljava/util/Iterator;",
                    shift = At.Shift.BEFORE
            ),
            remap = false,
            require = 0
    )
    private void rtcolony$uploadFocusLensUniforms(CallbackInfo ci) {
        RtsFocusLensShader.uploadToCurrentProgram();
    }

    @Inject(method = "getVisibleFaces", at = @At("HEAD"), cancellable = true, remap = false)
    private static void rtcolony$keepOrthographicFaces(
            int cameraX,
            int cameraY,
            int cameraZ,
            int sectionX,
            int sectionY,
            int sectionZ,
            CallbackInfoReturnable<Integer> ci
    ) {
        if (RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric()) {
            ci.setReturnValue(-1);
        }
    }
}
