package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refresh the section graph once when the projection changes. Ongoing frustum
 * refreshes are handled separately in {@link SectionOcclusionGraphMixin}; that
 * avoids a redirect into LevelRenderer, which Sodium also transforms.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;

    private boolean rtcolony$wasTrueIsometric;

    @Inject(method = "setupRender", at = @At("HEAD"))
    private void rtcolony$refreshSectionsForIsometric(Camera camera, Frustum frustum, boolean capturedFrustum,
                                                       boolean spectator, CallbackInfo ci) {
        boolean trueIsometric = RtsModeState.isEnabled() && RtsCameraState.isTrueIsometric();
        if (trueIsometric && !this.rtcolony$wasTrueIsometric) {
            this.sectionOcclusionGraph.invalidate();
        }
        this.rtcolony$wasTrueIsometric = trueIsometric;
    }
}
