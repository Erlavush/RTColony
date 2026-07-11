package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.erlavush.rtcolony.mixin.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds a target-aware clean terrain cutaway to Sodium/Iris terrain shaders.
 *
 * <p>The feature is enabled only while {@link RtsCutawayState} confirms that a
 * selected/followed entity is obstructed. Terrain pixels inside an
 * entity-sized screen-space ellipse and in front of the entity are discarded
 * completely. The ellipse opens and closes by changing size; the interior does
 * not use screen-door or stochastic transparency.</p>
 */
public final class RtsFocusLensShader {
    private static final String MARKER = "rtcolony_LensActive";
    private static final Pattern MAIN_FUNCTION =
            Pattern.compile("\\bvoid\\s+main\\s*\\([^)]*\\)\\s*\\{");

    private static final float DEPTH_EPSILON = 0.000002F;

    private static final String UNIFORMS = """

            // RTColony target-aware, ground-safe clean cutaway uniforms.
            uniform float rtcolony_LensActive;
            uniform vec2 rtcolony_LensCenter;
            uniform vec2 rtcolony_LensRadius;
            uniform float rtcolony_LensTargetDepth;
            uniform float rtcolony_LensFloorY;

            """;

    private static final String MAIN_PREFIX = """

                if (rtcolony_LensActive > 0.5
                        && gl_FragCoord.z < rtcolony_LensTargetDepth
                        && gl_FragCoord.y >= rtcolony_LensFloorY) {
                    vec2 rtcolony_LensSafeRadius = max(
                            rtcolony_LensRadius,
                            vec2(1.0)
                    );
                    vec2 rtcolony_LensNormalized = (
                            gl_FragCoord.xy - rtcolony_LensCenter
                    ) / rtcolony_LensSafeRadius;

                    if (dot(
                            rtcolony_LensNormalized,
                            rtcolony_LensNormalized
                    ) <= 1.0) {
                        discard;
                    }
                }
            """;

    private static boolean terrainShaderPatched;
    private static boolean loggedPatch;
    private static Method irisShadowMethod;
    private static boolean irisShadowMethodResolved;

    private static final Map<Integer, UniformLocations> UNIFORM_LOCATIONS =
            new HashMap<>();

    private RtsFocusLensShader() {
    }

    public static String patchTerrainFragment(String source) {
        if (source == null || source.contains(MARKER)) {
            return source;
        }

        Matcher matcher = MAIN_FUNCTION.matcher(source);
        if (!matcher.find()) {
            return source;
        }

        int mainStart = matcher.start();
        int bodyStart = matcher.end();

        String patched = source.substring(0, mainStart)
                + UNIFORMS
                + source.substring(mainStart, bodyStart)
                + MAIN_PREFIX
                + source.substring(bodyStart);

        UNIFORM_LOCATIONS.clear();
        markPatched();
        return patched;
    }

    public static Map<?, ?> patchIrisTerrainShaders(Map<?, ?> shaders) {
        if (shaders == null || shaders.isEmpty()) {
            return shaders;
        }

        Map<Object, Object> patched = new LinkedHashMap<>();
        boolean changed = false;

        for (Map.Entry<?, ?> entry : shaders.entrySet()) {
            Object value = entry.getValue();

            if ("FRAGMENT".equals(String.valueOf(entry.getKey()))
                    && value instanceof String source) {
                String patchedSource = patchTerrainFragment(source);
                patched.put(entry.getKey(), patchedSource);
                changed |= patchedSource != source;
            } else {
                patched.put(entry.getKey(), value);
            }
        }

        return changed ? patched : shaders;
    }

    /**
     * Keep this method name because Sodium culling/render mixins already use it.
     * It now means "a real entity cutaway is rendering", not merely "RTS mode
     * is enabled".
     */
    public static boolean isLensActive() {
        Minecraft minecraft = Minecraft.getInstance();

        return terrainShaderPatched
                && RtsCutawayState.isRendering()
                && RtsModeState.isEnabled()
                && RtsCameraState.isActive()
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null;
    }

    /**
     * Upload cutaway uniforms to the currently bound Sodium/Iris terrain
     * program.
     */
    public static void uploadToCurrentProgram() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) {
            return;
        }

        UniformLocations locations = UNIFORM_LOCATIONS.computeIfAbsent(
                program,
                UniformLocations::find
            );

        if (locations.active() < 0) {
            return;
        }

        LensUniforms uniforms = createUniforms();
        if (uniforms == null || isIrisShadowPass()) {
            GL20.glUniform1f(locations.active(), 0.0F);
            return;
        }

        GL20.glUniform1f(locations.active(), 1.0F);
        upload2f(
                locations.center(),
                uniforms.centerX(),
                uniforms.centerY()
        );
        upload2f(
                locations.radius(),
                uniforms.radiusX(),
                uniforms.radiusY()
        );
        upload1f(
                locations.targetDepth(),
                uniforms.targetDepth()
        );
        upload1f(
                locations.floorY(),
                uniforms.floorY()
        );
    }

    private static LensUniforms createUniforms() {
        if (!isLensActive()) {
            return null;
        }

        RtsCutawayState.Snapshot snapshot =
                RtsCutawayState.getSnapshot();

        if (snapshot == null) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();

        if (!camera.isInitialized()) {
            return null;
        }

        double fov = ((GameRendererAccessor) minecraft.gameRenderer)
                .rtcolony$getFov(
                        camera,
                        camera.getPartialTickTime(),
                        true
                );

        Matrix4f projection =
                minecraft.gameRenderer.getProjectionMatrix(fov);

        Quaternionf inverseCameraRotation =
                new Quaternionf(camera.rotation()).conjugate();

        int width = minecraft.getWindow().getScreenWidth();
        int height = minecraft.getWindow().getScreenHeight();

        if (width <= 0 || height <= 0) {
            return null;
        }

        ProjectedBounds projected = projectBounds(
                snapshot.targetBounds(),
                camera,
                inverseCameraRotation,
                projection,
                width,
                height
        );

        if (projected == null) {
            return null;
        }

        // The target must intersect the screen. Do not keep culling disabled
        // for an entity that has moved fully off-screen.
        if (projected.maxX() < 0.0F
                || projected.minX() > width
                || projected.maxY() < 0.0F
                || projected.minY() > height) {
            return null;
        }

        float minimumDimension = Math.min(width, height);
        float padding = Mth.clamp(
                minimumDimension * 0.028F,
                18.0F,
                44.0F
        );

        float minimumRadiusX = Mth.clamp(
                minimumDimension * 0.045F,
                38.0F,
                86.0F
        );
        float minimumRadiusY = Mth.clamp(
                minimumDimension * 0.060F,
                52.0F,
                112.0F
        );

        float maximumRadiusX = Mth.clamp(
                minimumDimension * 0.17F,
                100.0F,
                220.0F
        );
        float maximumRadiusY = Mth.clamp(
                minimumDimension * 0.22F,
                130.0F,
                280.0F
        );

        float radiusX = Mth.clamp(
                projected.width() * 0.5F + padding,
                minimumRadiusX,
                maximumRadiusX
        );

        float bodySpanAboveFeet = Math.max(
                1.0F,
                projected.maxY() - projected.maxFeetY()
        );

        float radiusY = Mth.clamp(
                bodySpanAboveFeet * 0.72F + padding,
                minimumRadiusY,
                maximumRadiusY
        );

        float animation = smoothStep(snapshot.openAmount());
        radiusX *= animation;
        radiusY *= animation;

        if (radiusX < 1.0F || radiusY < 1.0F) {
            return null;
        }

        float targetDepth = projected.upperBodyDepth() - DEPTH_EPSILON;

        float floorMargin = Mth.clamp(
                minimumDimension * 0.008F,
                4.0F,
                12.0F
        );

        float floorY = projected.maxFeetY() + floorMargin;

        if (!Float.isFinite(targetDepth)
                || targetDepth <= 0.0F
                || targetDepth >= 1.0F
                || !Float.isFinite(floorY)) {
            return null;
        }

        return new LensUniforms(
                projected.centerX(),
                projected.cutawayCenterY(),
                radiusX,
                radiusY,
                targetDepth,
                floorY
        );
    }

    private static ProjectedBounds projectBounds(
            AABB bounds,
            Camera camera,
            Quaternionf inverseCameraRotation,
            Matrix4f projection,
            int width,
            int height
    ) {
        if (bounds == null) {
            return null;
        }

        Vec3 boundsCenter = bounds.getCenter();
        double heightVal = Math.max(0.01D, bounds.getYsize());

        ProjectedPoint upperBody = projectPoint(
                new Vec3(
                        boundsCenter.x,
                        bounds.minY + heightVal * 0.62D,
                        boundsCenter.z
                ),
                camera,
                inverseCameraRotation,
                projection,
                width,
                height
        );

        if (upperBody == null) {
            return null;
        }

        ProjectedPoint[] feet = {
                projectPoint(
                        new Vec3(bounds.minX, bounds.minY, bounds.minZ),
                        camera,
                        inverseCameraRotation,
                        projection,
                        width,
                        height
                ),
                projectPoint(
                        new Vec3(bounds.minX, bounds.minY, bounds.maxZ),
                        camera,
                        inverseCameraRotation,
                        projection,
                        width,
                        height
                ),
                projectPoint(
                        new Vec3(bounds.maxX, bounds.minY, bounds.minZ),
                        camera,
                        inverseCameraRotation,
                        projection,
                        width,
                        height
                ),
                projectPoint(
                        new Vec3(bounds.maxX, bounds.minY, bounds.maxZ),
                        camera,
                        inverseCameraRotation,
                        projection,
                        width,
                        height
                )
        };

        float maxFeetY = Float.NEGATIVE_INFINITY;
        for (ProjectedPoint foot : feet) {
            if (foot != null) {
                maxFeetY = Math.max(maxFeetY, foot.screenY());
            }
        }

        if (!Float.isFinite(maxFeetY)) {
            return null;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        int validPoints = 0;

        double[] xValues = {bounds.minX, bounds.maxX};
        double[] yValues = {bounds.minY, bounds.maxY};
        double[] zValues = {bounds.minZ, bounds.maxZ};

        for (double x : xValues) {
            for (double y : yValues) {
                for (double z : zValues) {
                    ProjectedPoint point = projectPoint(
                            new Vec3(x, y, z),
                            camera,
                            inverseCameraRotation,
                            projection,
                            width,
                            height
                    );

                    if (point == null) {
                        continue;
                    }

                    minX = Math.min(minX, point.screenX());
                    minY = Math.min(minY, point.screenY());
                    maxX = Math.max(maxX, point.screenX());
                    maxY = Math.max(maxY, point.screenY());
                    validPoints++;
                }
            }
        }

        if (validPoints == 0
                || !Float.isFinite(minX)
                || !Float.isFinite(minY)
                || !Float.isFinite(maxX)
                || !Float.isFinite(maxY)) {
            return null;
        }

        return new ProjectedBounds(
                minX,
                minY,
                maxX,
                maxY,
                upperBody.depth(),
                maxFeetY
        );
    }

    private static ProjectedPoint projectPoint(
            Vec3 point,
            Camera camera,
            Quaternionf inverseCameraRotation,
            Matrix4f projection,
            int width,
            int height
    ) {
        Vector4f clip = project(
                point,
                camera,
                inverseCameraRotation,
                projection
        );

        if (!Float.isFinite(clip.w())
                || clip.w() <= 1.0E-6F) {
            return null;
        }

        float inverseW = 1.0F / clip.w();
        float ndcX = clip.x() * inverseW;
        float ndcY = clip.y() * inverseW;
        float depth = clip.z() * inverseW * 0.5F + 0.5F;

        if (!Float.isFinite(ndcX)
                || !Float.isFinite(ndcY)
                || !Float.isFinite(depth)) {
            return null;
        }

        return new ProjectedPoint(
                (ndcX * 0.5F + 0.5F) * width,
                (ndcY * 0.5F + 0.5F) * height,
                depth
        );
    }

    private static Vector4f project(
            Vec3 target,
            Camera camera,
            Quaternionf inverseCameraRotation,
            Matrix4f projection
    ) {
        Vec3 relative = target.subtract(camera.getPosition());

        Vector3f viewSpace = new Vector3f(
                (float) relative.x,
                (float) relative.y,
                (float) relative.z
        ).rotate(inverseCameraRotation);

        return new Vector4f(
                viewSpace,
                1.0F
        ).mul(projection);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static void upload1f(int location, float value) {
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private static void upload2f(
            int location,
            float x,
            float y
    ) {
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    private static boolean isIrisShadowPass() {
        if (!irisShadowMethodResolved) {
            irisShadowMethodResolved = true;

            try {
                Class<?> shadowState = Class.forName(
                        "net.irisshaders.iris.shadows.ShadowRenderingState"
                );
                irisShadowMethod = shadowState.getMethod(
                        "areShadowsCurrentlyBeingRendered"
                );
            } catch (ReflectiveOperationException ignored) {
                irisShadowMethod = null;
            }
        }

        if (irisShadowMethod == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(
                    irisShadowMethod.invoke(null)
            );
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void markPatched() {
        terrainShaderPatched = true;

        if (!loggedPatch) {
            loggedPatch = true;
            RTColony.LOGGER.info(
                    "Enabled target-aware clean RTS entity cutaway"
            );
        }
    }

    private record LensUniforms(
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float targetDepth,
            float floorY
    ) {
    }

    private record ProjectedPoint(
            float screenX,
            float screenY,
            float depth
    ) {
    }

    private record ProjectedBounds(
            float minX,
            float minY,
            float maxX,
            float maxY,
            float upperBodyDepth,
            float maxFeetY
    ) {
        private float width() {
            return Math.max(0.0F, maxX - minX);
        }

        private float height() {
            return Math.max(0.0F, maxY - minY);
        }

        private float centerX() {
            return (minX + maxX) * 0.5F;
        }

        private float cutawayCenterY() {
            float lower = maxFeetY;
            float upper = maxY;
            return lower + (upper - lower) * 0.58F;
        }
    }

    private record UniformLocations(
            int active,
            int center,
            int radius,
            int targetDepth,
            int floorY
    ) {
        private static UniformLocations find(int program) {
            return new UniformLocations(
                    GL20.glGetUniformLocation(
                            program,
                            MARKER
                    ),
                    GL20.glGetUniformLocation(
                            program,
                            "rtcolony_LensCenter"
                    ),
                    GL20.glGetUniformLocation(
                            program,
                            "rtcolony_LensRadius"
                    ),
                    GL20.glGetUniformLocation(
                            program,
                            "rtcolony_LensTargetDepth"
                    ),
                    GL20.glGetUniformLocation(
                            program,
                            "rtcolony_LensFloorY"
                    )
            );
        }
    }
}
