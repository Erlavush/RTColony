package com.erlavush.rtcolony.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Entity portrait rendering adapted from Reign of Nether's PortraitRendererUnit
 * and PortraitRendererModifiers.
 */
final class RtsEntityPortraitRenderer {
    private static final int SIZE = 46;
    private static final int OFFSET_X = 31;
    private static final int OFFSET_Y = 105;
    private static final float STANDARD_EYE_HEIGHT = 1.74F;
    private static final int TICKS_LEFT_MIN = 90;
    private static final int TICKS_LEFT_MAX = 150;
    private static final int LOOK_RANGE_X = 18;
    private static final int LOOK_RANGE_Y = 0;
    private static final Set<String> MINECOLONIES_RAIDER_KEYWORDS = Set.of(
            "amazon",
            "barbarian",
            "mummy",
            "norsemen",
            "pharao",
            "pirate",
            "shieldmaiden"
    );

    private static int animatedEntityId = Integer.MIN_VALUE;
    private static int lookX;
    private static int lookY;
    private static int lastLookTargetX;
    private static int lastLookTargetY;
    private static int lookTargetX;
    private static int lookTargetY;
    private static int ticksLeft;

    private RtsEntityPortraitRenderer() {
    }

    private record PortraitModifier(int yOffset, int scaleOffset) {}

    static void tick() {
        RtsTargetingState.TargetSnapshot selected = RtsTargetingState.getSelectedTarget();
        if (selected == null || !(selected.entity() instanceof LivingEntity livingEntity) || livingEntity.isRemoved()) {
            animatedEntityId = Integer.MIN_VALUE;
            return;
        }

        ensureAnimationEntity(livingEntity);
        tickAnimation();
    }

    private static PortraitModifier getPortraitModifier(LivingEntity entity) {
        int yOffset = 0;
        int scaleOffset = 0;
        PortraitModifier mineColoniesModifier = getMineColoniesPortraitModifier(entity);
        if (mineColoniesModifier != null) {
            return mineColoniesModifier;
        }

        if (entity instanceof Warden) {
            yOffset = -60;
            scaleOffset = -21;
        } else if (entity instanceof Horse || entity instanceof SkeletonHorse || entity instanceof ZombieHorse) {
            yOffset = -6;
            scaleOffset = -10;
        } else if (entity instanceof Panda) {
            yOffset = -15;
            scaleOffset = -15;
        } else if (entity instanceof PolarBear) {
            yOffset = -15;
            scaleOffset = -15;
        } else if (entity instanceof Pig) {
            yOffset = 10;
        } else if (entity instanceof IronGolem) {
            yOffset = -54;
            scaleOffset = -17;
        } else if (entity instanceof AbstractFish) {
            yOffset = 20;
        } else if (entity instanceof Squid) {
            yOffset = 10;
            scaleOffset = -20;
        } else if (entity instanceof Turtle) {
            yOffset = 14;
            scaleOffset = -14;
        } else if (entity instanceof CaveSpider) {
            yOffset = 9;
            scaleOffset = -11;
        } else if (entity instanceof Spider) {
            scaleOffset = -18;
        } else if (entity instanceof Rabbit) {
            yOffset = 18;
            scaleOffset = 15;
        } else if (entity instanceof Chicken) {
            yOffset = 14;
        } else if (entity instanceof Blaze) {
            yOffset = -10;
            scaleOffset = -5;
        } else if (entity instanceof MushroomCow) {
            scaleOffset = -5;
        } else if (entity instanceof Donkey || entity instanceof Mule) {
            scaleOffset = -5;
        } else if (entity instanceof Ocelot || entity instanceof Cat) {
            yOffset = 7;
        } else if (entity instanceof Fox) {
            yOffset = 20;
        } else if (entity instanceof Vex) {
            yOffset = 5;
        } else if (entity instanceof Phantom) {
            yOffset = 20;
        } else if (entity instanceof Hoglin || entity instanceof Zoglin) {
            yOffset = -18;
            scaleOffset = -18;
        } else if (entity instanceof Slime slime) {
            yOffset = 33 - 17 * slime.getSize();
            scaleOffset = -32;
        } else if (entity instanceof Wolf) {
            yOffset = 12;
        } else if (entity instanceof Silverfish) {
            yOffset = 26;
        } else if (entity instanceof EnderMan) {
            yOffset = -15;
        } else if (entity instanceof Ravager) {
            yOffset = -54;
            scaleOffset = -25;
        } else if (entity instanceof Dolphin) {
            yOffset = 20;
            scaleOffset = -10;
        } else if (entity instanceof Bee) {
            yOffset = 20;
            scaleOffset = -5;
        } else if (entity instanceof WitherSkeleton) {
            yOffset = -15;
            scaleOffset = -4;
        } else if (entity instanceof Ghast) {
            yOffset = -118;
            scaleOffset = -37;
        }

        return new PortraitModifier(yOffset, scaleOffset);
    }

    private static PortraitModifier getMineColoniesPortraitModifier(LivingEntity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!"minecolonies".equals(entityId.getNamespace())) {
            return null;
        }

        String path = entityId.getPath();
        boolean isRaider = MINECOLONIES_RAIDER_KEYWORDS.stream().anyMatch(path::contains);
        if (!isRaider) {
            return null;
        }

        if (path.contains("chief")) {
            return new PortraitModifier(-16, -12);
        }
        return new PortraitModifier(-9, -8);
    }

    static void render(GuiGraphics guiGraphics, LivingEntity entity, int x, int y) {
        ensureAnimationEntity(entity);
        PortraitModifier modifier = getPortraitModifier(entity);
        int renderX = x + OFFSET_X;
        int renderY = y
                + (int) (entity.getEyeHeight() / STANDARD_EYE_HEIGHT * OFFSET_Y)
                + modifier.yOffset()
                + getBabyYOffset(entity);
        int sizeFinal = SIZE + modifier.scaleOffset();

        drawEntityOnScreen(guiGraphics.pose(), entity, renderX, renderY, sizeFinal);
    }

    private static int getBabyYOffset(LivingEntity entity) {
        if (!entity.isBaby()) {
            return 0;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if ("villager".equals(entityId.getPath())) {
            return 10;
        }
        if (entity instanceof Animal) {
            return 20;
        }
        return 10;
    }

    private static void ensureAnimationEntity(LivingEntity entity) {
        if (animatedEntityId == entity.getId()) {
            return;
        }

        animatedEntityId = entity.getId();
        lookX = 0;
        lookY = 0;
        lastLookTargetX = 0;
        lastLookTargetY = 0;
        lookTargetX = 0;
        lookTargetY = 0;
        randomiseAnimation(true);
    }

    private static void randomiseAnimation(boolean randomisePos) {
        if (randomisePos) {
            lookX = randRangeInt(-LOOK_RANGE_X, LOOK_RANGE_X);
            lookY = randRangeInt(-LOOK_RANGE_Y, LOOK_RANGE_Y);
        }
        ticksLeft = randRangeInt(TICKS_LEFT_MIN, TICKS_LEFT_MAX);

        lastLookTargetX = lookTargetX;
        lastLookTargetY = lookTargetY;

        while (Math.abs(lookTargetX - lookX) < LOOK_RANGE_X / 2) {
            lookTargetX = randRangeInt(-LOOK_RANGE_X, LOOK_RANGE_X);
        }
        while (Math.abs(lookTargetY - lookY) < LOOK_RANGE_Y / 2) {
            lookTargetY = randRangeInt(-LOOK_RANGE_Y, LOOK_RANGE_Y);
        }
    }

    private static void tickAnimation() {
        ticksLeft -= 1;
        if (ticksLeft <= 0) {
            randomiseAnimation(false);
        }

        int lookSpeedX = Math.max(1, Math.abs(lastLookTargetX - lookX) / 20);
        int lookSpeedY = Math.max(1, Math.abs(lastLookTargetY - lookY) / 20);

        if (lookX < lookTargetX) {
            lookX += lookSpeedX;
        }
        if (lookX > lookTargetX) {
            lookX -= lookSpeedX;
        }
        if (lookY < lookTargetY) {
            lookY += lookSpeedY;
        }
        if (lookY > lookTargetY) {
            lookY -= lookSpeedY;
        }

        if (Math.abs(lookTargetX - lookX) < lookSpeedX) {
            lookX = lookTargetX;
        }
        if (Math.abs(lookTargetY - lookY) < lookSpeedY) {
            lookY = lookTargetY;
        }
    }

    private static int randRangeInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private static void drawEntityOnScreen(PoseStack poseStack, LivingEntity entity, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        float yawRadians = (float) Math.atan(-lookX / 40.0F);
        float pitchRadians = (float) Math.atan(-lookY / 40.0F);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRotationSnapshot snapshot = EntityRotationSnapshot.capture(entity);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        boolean posePushed = false;
        modelViewStack.pushMatrix();

        try {
            modelViewStack.translate((float) x, (float) y, 1050.0F);
            modelViewStack.scale(1.0F, 1.0F, -1.0F);
            RenderSystem.applyModelViewMatrix();

            poseStack.pushPose();
            posePushed = true;
            poseStack.translate(0.0D, 0.0D, 10.0D);
            poseStack.scale((float) size, (float) size, (float) size);

            Quaternionf baseRotation = Axis.ZP.rotationDegrees(180.0F);
            Quaternionf pitchRotation = Axis.XP.rotationDegrees(pitchRadians * 20.0F);
            baseRotation.mul(pitchRotation);
            poseStack.mulPose(baseRotation);

            applyPortraitRotation(entity, yawRadians, pitchRadians);

            Lighting.setupForEntityInInventory();
            dispatcher.setRenderShadow(false);
            pitchRotation.conjugate();
            dispatcher.overrideCameraOrientation(pitchRotation);

            RenderSystem.runAsFancy(() -> {
                try {
                    MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
                    dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, bufferSource, 15728880);
                    bufferSource.endBatch();
                } catch (RuntimeException ignored) {
                }
            });
        } finally {
            dispatcher.setRenderShadow(true);
            snapshot.restore(entity);
            modelViewStack.popMatrix();
            if (posePushed) {
                poseStack.popPose();
            }
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
        }
    }

    private static void applyPortraitRotation(LivingEntity entity, float yawRadians, float pitchRadians) {
        entity.yBodyRot = 180.0F + yawRadians * 20.0F;
        entity.setYRot(180.0F + yawRadians * 40.0F);
        entity.setXRot(-pitchRadians * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
    }

    private record EntityRotationSnapshot(
            float yRot,
            float xRot,
            float yBodyRot,
            float yBodyRotO,
            float yHeadRot,
            float yHeadRotO
    ) {
        private static EntityRotationSnapshot capture(LivingEntity entity) {
            return new EntityRotationSnapshot(
                    entity.getYRot(),
                    entity.getXRot(),
                    entity.yBodyRot,
                    entity.yBodyRotO,
                    entity.yHeadRot,
                    entity.yHeadRotO
            );
        }

        private void restore(LivingEntity entity) {
            entity.setYRot(this.yRot);
            entity.setXRot(this.xRot);
            entity.yBodyRot = this.yBodyRot;
            entity.yBodyRotO = this.yBodyRotO;
            entity.yHeadRot = this.yHeadRot;
            entity.yHeadRotO = this.yHeadRotO;
        }
    }
}
