package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsModeState;
import com.erlavush.rtcolony.client.RtsTargetingState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public Options options;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void rtcolony$drainGameplayKeybinds(CallbackInfo ci) {
        if (!RtsModeState.isEnabled()) {
            return;
        }

        drain(this.options.keyDrop);
        drain(this.options.keyAttack);
        drain(this.options.keyUse);
        drain(this.options.keyPickItem);
        drain(this.options.keySwapOffhand);
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void rtcolony$outlineSelectedAndHoveredEntity(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (RtsTargetingState.shouldRenderEntityOutline(entity)) {
            cir.setReturnValue(true);
        }
    }

    private static void drain(KeyMapping keyMapping) {
        while (keyMapping.consumeClick()) {
        }
    }
}
