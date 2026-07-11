package com.erlavush.rtcolony.mixin;

import com.erlavush.rtcolony.client.RtsCameraState;
import com.erlavush.rtcolony.client.RtsBuildDrawer;
import com.erlavush.rtcolony.client.RtsModeState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch, float roll);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void rtcolony$setupRtsCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (!RtsModeState.isEnabled() || !RtsCameraState.isActive()) {
            return;
        }

        this.detached = true;
        RtsCameraState.advanceRenderState();
        this.setRotation(RtsCameraState.getYaw(), RtsCameraState.getPitch(), 0.0F);
        this.setPosition(RtsBuildDrawer.isPlacementLocked()
                ? RtsCameraState.getPlacementCameraPosition(level)
                : RtsCameraState.getCameraPosition());
    }
}
