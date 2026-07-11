package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsFocusLensShader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds RTColony's fixed focus lens to Sodium's built-in terrain shader. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class SodiumShaderLoaderMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void rtcolony$addFocusLens(
            ResourceLocation location,
            CallbackInfoReturnable<String> ci
    ) {
        if ("sodium".equals(location.getNamespace())
                && "blocks/block_layer_opaque.fsh".equals(location.getPath())) {
            ci.setReturnValue(RtsFocusLensShader.patchTerrainFragment(ci.getReturnValue()));
        }
    }
}
