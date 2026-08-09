package com.erlavush.rtcolony.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Orthographic weather renderer based on vanilla LevelRenderer weather.
 *
 * <p>Vanilla emits precipitation in a fixed five/ten-block circle around the
 * camera. That circle becomes visible when RTColony zooms an orthographic view
 * out. This renderer samples the complete isometric viewport footprint at a
 * zoom-aware density so rain and snow continue to cover the screen without
 * generating hundreds of thousands of columns at maximum zoom.</p>
 */
public final class RtsIsometricWeatherRenderer {
    private static final ResourceLocation RAIN_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation SNOW_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    private static final int TARGET_SAMPLES_ACROSS = 52;
    private static final double EDGE_MARGIN = 6.0D;
    private static final double HALF_STREAK_WIDTH = 0.16D;

    private RtsIsometricWeatherRenderer() {
    }

    public static boolean render(
            LightTexture lightTexture,
            float partialTick,
            double camX,
            double camY,
            double camZ
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RtsModeState.isEnabled()
                || !RtsCameraState.isTrueIsometric()
                || minecraft.level == null) {
            return false;
        }

        float rainLevel = minecraft.level.getRainLevel(partialTick);
        if (rainLevel <= 0.0F) {
            return true;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double halfHorizontal = RtsCameraState.getIsometricHorizontalSpan(minecraft) * 0.5D;
        double halfVertical = RtsCameraState.getIsometricVerticalSpan() * 0.5D;
        double extentX = Math.abs(left.x()) * halfHorizontal
                + Math.abs(up.x()) * halfVertical
                + EDGE_MARGIN;
        double extentZ = Math.abs(left.z()) * halfHorizontal
                + Math.abs(up.z()) * halfVertical
                + EDGE_MARGIN;

        Vec3 center = RtsCameraState.getCenter();
        int minX = Mth.floor(center.x - extentX);
        int maxX = Mth.ceil(center.x + extentX);
        int minZ = Mth.floor(center.z - extentZ);
        int maxZ = Mth.ceil(center.z + extentZ);
        int widestDimension = Math.max(maxX - minX + 1, maxZ - minZ + 1);
        int step = Math.max(1, Mth.ceil((float) widestDimension / TARGET_SAMPLES_ACROSS));

        double verticalReach = Math.max(24.0D, RtsCameraState.getIsometricVerticalSpan());
        int lowerY = Mth.floor(center.y - verticalReach);
        int upperY = Mth.ceil(center.y + verticalReach);
        long ticks = minecraft.level.getGameTime();

        lightTexture.turnOnLightLayer();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);

        PrecipitationColumns columns = collectPrecipitationColumns(
                minecraft,
                minX,
                maxX,
                minZ,
                maxZ,
                lowerY,
                upperY,
                step
        );
        renderPrecipitationType(
                minecraft,
                partialTick,
                camX,
                camY,
                camZ,
                columns.rain(),
                ticks,
                rainLevel,
                Biome.Precipitation.RAIN,
                RAIN_LOCATION
        );
        renderPrecipitationType(
                minecraft,
                partialTick,
                camX,
                camY,
                camZ,
                columns.snow(),
                ticks,
                rainLevel,
                Biome.Precipitation.SNOW,
                SNOW_LOCATION
        );

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
        return true;
    }

    private static void renderPrecipitationType(
            Minecraft minecraft,
            float partialTick,
            double camX,
            double camY,
            double camZ,
            List<PrecipitationColumn> columns,
            long ticks,
            float rainLevel,
            Biome.Precipitation requestedType,
            ResourceLocation texture
    ) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = null;
        Vector3f left = minecraft.gameRenderer.getMainCamera().getLeftVector();
        double widthX = -left.x() * HALF_STREAK_WIDTH;
        double widthZ = -left.z() * HALF_STREAK_WIDTH;

        for (PrecipitationColumn column : columns) {
            if (builder == null) {
                RenderSystem.setShaderTexture(0, texture);
                builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            float speed = requestedType == Biome.Precipitation.RAIN ? column.rainSpeed() : 0.25F;
            float vOffset = -((float) (ticks & 131071L) + partialTick) / 32.0F * speed;
            float alpha = rainLevel * (requestedType == Biome.Precipitation.RAIN ? 0.78F : 0.68F);
            double baseX = column.x() + 0.5D - camX;
            double baseZ = column.z() + 0.5D - camZ;
            float bottomV = column.bottomY() * 0.25F + vOffset;
            float topV = column.topY() * 0.25F + vOffset;

            builder.addVertex((float) (baseX - widthX), (float) (column.topY() - camY), (float) (baseZ - widthZ))
                    .setUv(0.0F, bottomV).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(column.light());
            builder.addVertex((float) (baseX + widthX), (float) (column.topY() - camY), (float) (baseZ + widthZ))
                    .setUv(1.0F, bottomV).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(column.light());
            builder.addVertex((float) (baseX + widthX), (float) (column.bottomY() - camY), (float) (baseZ + widthZ))
                    .setUv(1.0F, topV).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(column.light());
            builder.addVertex((float) (baseX - widthX), (float) (column.bottomY() - camY), (float) (baseZ - widthZ))
                    .setUv(0.0F, topV).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(column.light());
        }

        if (builder != null) {
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }
    }

    private static PrecipitationColumns collectPrecipitationColumns(
            Minecraft minecraft,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int lowerY,
            int upperY,
            int step
    ) {
        List<PrecipitationColumn> rain = new ArrayList<>();
        List<PrecipitationColumn> snow = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int sampleY = Mth.floor(RtsCameraState.getCenter().y);
        int startX = Math.floorDiv(minX, step) * step;
        int startZ = Math.floorDiv(minZ, step) * step;

        for (int cellZ = startZ; cellZ <= maxZ; cellZ += step) {
            for (int cellX = startX; cellX <= maxX; cellX += step) {
                RandomSource random = RandomSource.create(sampleSeed(cellX, cellZ));
                int x = cellX + random.nextInt(step);
                int z = cellZ + random.nextInt(step);
                if (x < minX || x > maxX || z < minZ || z > maxZ) {
                    continue;
                }

                mutable.set(x, sampleY, z);
                Biome biome = minecraft.level.getBiome(mutable).value();
                if (!biome.hasPrecipitation()) {
                    continue;
                }

                int surfaceY = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                mutable.set(x, surfaceY, z);
                Biome.Precipitation type = biome.getPrecipitationAt(mutable);
                if (type != Biome.Precipitation.RAIN && type != Biome.Precipitation.SNOW) {
                    continue;
                }
                int bottomY = Math.max(surfaceY, lowerY);
                int topY = Math.max(bottomY + 4, upperY);
                int light = LevelRenderer.getLightColor(minecraft.level, mutable);
                PrecipitationColumn column = new PrecipitationColumn(
                        x,
                        z,
                        bottomY,
                        topY,
                        light,
                        type == Biome.Precipitation.RAIN ? 3.0F + random.nextFloat() : 0.25F
                );
                if (type == Biome.Precipitation.RAIN) {
                    rain.add(column);
                } else if (type == Biome.Precipitation.SNOW) {
                    snow.add(column);
                }
            }
        }

        return new PrecipitationColumns(rain, snow);
    }

    private static long sampleSeed(int x, int z) {
        return (long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761);
    }

    private record PrecipitationColumn(
            int x,
            int z,
            int bottomY,
            int topY,
            int light,
            float rainSpeed
    ) {
    }

    private record PrecipitationColumns(
            List<PrecipitationColumn> rain,
            List<PrecipitationColumn> snow
    ) {
    }
}
