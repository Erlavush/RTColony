package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsSodiumCutaway;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels cutaway block models at Sodium chunk-mesh compilation time.
 * Adapted from Dungeons Perspective's MixinBlockRenderer.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class SodiumBlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void rtcolony$cullCutawayModel(
            BakedModel model,
            BlockState state,
            BlockPos pos,
            BlockPos origin,
            CallbackInfo ci
    ) {
        if (RtsSodiumCutaway.shouldCull(pos, state)) {
            ci.cancel();
        }
    }
}
