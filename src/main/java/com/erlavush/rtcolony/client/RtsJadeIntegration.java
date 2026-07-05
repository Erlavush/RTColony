package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import snownee.jade.api.Accessor;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.overlay.RayTracing;

final class RtsJadeIntegration {
    private static final int CALLBACK_PRIORITY = 10_000;
    private static boolean registered;

    private RtsJadeIntegration() {
    }

    static void register() {
        if (registered) {
            return;
        }

        WailaClientRegistration registration = WailaClientRegistration.instance();
        registration.addRayTraceCallback(CALLBACK_PRIORITY, RtsJadeIntegration::overrideRayTrace);
        registration.rayTraceCallback.sort();
        registered = true;
        RTColony.LOGGER.info("Registered RTColony Jade ray trace integration");
    }

    private static Accessor<?> overrideRayTrace(HitResult jadeHit, Accessor<?> accessor, Accessor<?> originalAccessor) {
        if (!RtsModeState.isEnabled() || !RtsCameraState.isActive()) {
            return accessor;
        }

        HitResult rtsHit = RtsTargetingState.getHoverHit();
        if (rtsHit == null || rtsHit.getType() == HitResult.Type.MISS) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }

        WailaClientRegistration registration = WailaClientRegistration.instance();
        if (rtsHit instanceof EntityHitResult entityHit) {
            if (registration.shouldHide(entityHit.getEntity())) {
                return null;
            }

            return registration.entityAccessor()
                    .level(minecraft.level)
                    .player(minecraft.player)
                    .hit(entityHit)
                    .entity(entityHit.getEntity())
                    .requireVerification()
                    .build();
        }

        if (rtsHit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState blockState = RayTracing.wrapBlock(
                    minecraft.level,
                    blockHit,
                    CollisionContext.of(minecraft.player)
            );
            if (registration.shouldHide(blockState)) {
                return null;
            }

            return registration.blockAccessor()
                    .level(minecraft.level)
                    .player(minecraft.player)
                    .blockState(blockState)
                    .blockEntity(minecraft.level.getBlockEntity(blockHit.getBlockPos()))
                    .hit(blockHit)
                    .requireVerification()
                    .build();
        }

        return null;
    }
}
