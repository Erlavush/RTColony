package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsFocusLensShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** Adds the same focus lens after Iris has transformed shader-pack terrain programs. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.programs.SodiumPrograms", remap = false)
public abstract class IrisSodiumProgramsMixin {
    @Inject(method = "transformShaders", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void rtcolony$addFocusLens(CallbackInfoReturnable<Map<?, ?>> ci) {
        ci.setReturnValue(RtsFocusLensShader.patchIrisTerrainShaders(ci.getReturnValue()));
    }
}
