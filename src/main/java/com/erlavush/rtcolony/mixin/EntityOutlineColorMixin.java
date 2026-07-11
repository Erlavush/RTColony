package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsTargetingState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies RTColony's selected/hovered colors to vanilla's silhouette pass. */
@Mixin(Entity.class)
public abstract class EntityOutlineColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void rtcolony$outlineColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        if (RtsTargetingState.isSelectedEntity(entity)) {
            cir.setReturnValue(0xFFFFFF);
        } else if (RtsTargetingState.shouldRenderEntityOutline(entity)) {
            cir.setReturnValue(0xFFFF55);
        }
    }
}
