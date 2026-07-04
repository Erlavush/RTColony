package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public Options options;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void rtcolony$drainGameplayKeybinds(CallbackInfo ci) {
        if (!RtsModeState.isEnabled()) {
            return;
        }

        drain(this.options.keyInventory);
        drain(this.options.keyDrop);
        drain(this.options.keyAttack);
        drain(this.options.keyUse);
        drain(this.options.keyPickItem);
        drain(this.options.keySwapOffhand);
    }

    private static void drain(KeyMapping keyMapping) {
        while (keyMapping.consumeClick()) {
        }
    }
}
