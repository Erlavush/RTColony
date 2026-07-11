package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.erlavush.rtcolony.mixin.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
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
 * Adds a fixed, screen-space visibility lens to Sodium terrain shaders.
 *
 * <p>The opaque terrain pass cannot use ordinary alpha blending without
 * sorting every block face. Instead, the lens uses a stable screen-door
 * pattern: foreground pixels are discarded while the remaining pixels keep
 * the wall readable. Geometry behind the wall is therefore still rendered,
 * without rebuilding chunks or making whole blocks pop in and out.</p>
 */
public final class RtsFocusLensShader {
    private static final String MARKER = "rtcolony_LensActive";
    private static final Pattern MAIN_FUNCTION = Pattern.compile("\\bvoid\\s+main\\s*\\([^)]*\\)\\s*\\{");
    private static final float OUTER_RADIUS_SCREEN_FRACTION = 0.23F;
    private static final float INNER_RADIUS_FRACTION = 0.64F;
    private static final float MAX_TRANSPARENCY = 0.62F;
    private static final float DEPTH_EPSILON = 0.000002F;

    private static final String UNIFORMS = """

            // RTColony fixed focus-lens uniforms.
            uniform float rtcolony_LensActive;
            uniform vec2 rtcolony_LensCenter;
            uniform float rtcolony_LensRadius;
            uniform float rtcolony_LensFeather;
            uniform float rtcolony_LensTargetDepth;
            uniform float rtcolony_LensStrength;

            """;

    private static final String MAIN_PREFIX = """

                if (rtcolony_LensActive > 0.5
                        && gl_FragCoord.z < rtcolony_LensTargetDepth) {
                    float rtcolony_LensDistance = distance(gl_FragCoord.xy, rtcolony_LensCenter);
                    float rtcolony_LensMask = 1.0 - smoothstep(
                            rtcolony_LensRadius - rtcolony_LensFeather,
                            rtcolony_LensRadius,
                            rtcolony_LensDistance
                    );
                    float rtcolony_LensPattern = fract(
                            52.9829189 * fract(dot(
                                    floor(gl_FragCoord.xy),
                                    vec2(0.06711056, 0.00583715)
                            ))
                    );
                    if (rtcolony_LensPattern < rtcolony_LensMask * rtcolony_LensStrength) {
                        discard;
                    }
                }
            """;

    private static boolean terrainShaderPatched;
    private static boolean loggedPatch;
    private static Method irisShadowMethod;
    private static boolean irisShadowMethodResolved;
    private static final Map<Integer, UniformLocations> UNIFORM_LOCATIONS = new HashMap<>();

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
            if ("FRAGMENT".equals(String.valueOf(entry.getKey())) && value instanceof String source) {
                String patchedSource = patchTerrainFragment(source);
                patched.put(entry.getKey(), patchedSource);
                changed |= patchedSource != source;
            } else {
                patched.put(entry.getKey(), value);
            }
        }
        return changed ? patched : shaders;
    }

    public static boolean isLensActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return terrainShaderPatched
                && RtsModeState.isEnabled()
                && RtsCameraState.isActive()
                && minecraft.level != null
                && minecraft.player != null;
    }

    /** Uploads lens uniforms to the currently bound Sodium/Iris terrain program. */
    public static void uploadToCurrentProgram() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) {
            return;
        }

        UniformLocations locations = UNIFORM_LOCATIONS.computeIfAbsent(program, UniformLocations::find);
        if (locations.active() < 0) {
            return;
        }

        LensUniforms uniforms = createUniforms();
        if (uniforms == null || isIrisShadowPass()) {
            GL20.glUniform1f(locations.active(), 0.0F);
            return;
        }

        GL20.glUniform1f(locations.active(), 1.0F);
        upload2f(locations.center(), uniforms.centerX(), uniforms.centerY());
        upload1f(locations.radius(), uniforms.radius());
        upload1f(locations.feather(), uniforms.feather());
        upload1f(locations.targetDepth(), uniforms.targetDepth());
        upload1f(locations.strength(), MAX_TRANSPARENCY);
    }

    private static LensUniforms createUniforms() {
        if (!isLensActive()) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return null;
        }

        double fov = ((GameRendererAccessor) minecraft.gameRenderer)
                .rtcolony$getFov(camera, camera.getPartialTickTime(), true);
        Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix(fov);
        Quaternionf inverseCameraRotation = new Quaternionf(camera.rotation()).conjugate();
        int width = minecraft.getWindow().getScreenWidth();
        int height = minecraft.getWindow().getScreenHeight();
        float radius = Math.max(96.0F, Math.min(width, height) * OUTER_RADIUS_SCREEN_FRACTION);

        Vec3 target = selectLensTarget(
                minecraft,
                camera,
                inverseCameraRotation,
                projection,
                width,
                height,
                radius
        );
        Vector4f clip = project(target, camera, inverseCameraRotation, projection);
        if (!Float.isFinite(clip.w()) || Math.abs(clip.w()) < 1.0E-6F) {
            return null;
        }

        float targetDepth = clip.z() / clip.w() * 0.5F + 0.5F - DEPTH_EPSILON;
        if (!Float.isFinite(targetDepth) || targetDepth <= 0.0F || targetDepth >= 1.0F) {
            return null;
        }

        return new LensUniforms(
                width * 0.5F,
                height * 0.5F,
                radius,
                radius * (1.0F - INNER_RADIUS_FRACTION),
                targetDepth
        );
    }

    private static Vec3 selectLensTarget(
            Minecraft minecraft,
            Camera camera,
            Quaternionf inverseCameraRotation,
            Matrix4f projection,
            int width,
            int height,
            float radius
    ) {
        Entity followed = RtsTargetingState.getFollowedEntity(minecraft);
        Vec3 followedCenter = entityCenter(followed);
        if (isInsideLens(followedCenter, camera, inverseCameraRotation, projection, width, height, radius)) {
            return followedCenter;
        }

        RtsTargetingState.TargetSnapshot selected = RtsTargetingState.getSelectedTarget();
        Vec3 selectedCenter = selected == null ? null : entityCenter(selected.entity());
        if (isInsideLens(selectedCenter, camera, inverseCameraRotation, projection, width, height, radius)) {
            return selectedCenter;
        }

        Vec3 playerCenter = entityCenter(minecraft.player);
        if (isInsideLens(playerCenter, camera, inverseCameraRotation, projection, width, height, radius)) {
            return playerCenter;
        }

        return RtsCameraState.getCenter();
    }

    private static Vec3 entityCenter(Entity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            return null;
        }
        return entity.getBoundingBox().getCenter();
    }

    private static boolean isInsideLens(
            Vec3 target,
            Camera camera,
            Quaternionf inverseCameraRotation,
            Matrix4f projection,
            int width,
            int height,
            float radius
    ) {
        if (target == null) {
            return false;
        }

        Vector4f clip = project(target, camera, inverseCameraRotation, projection);
        if (!Float.isFinite(clip.w()) || clip.w() <= 1.0E-6F) {
            return false;
        }

        float screenX = (clip.x() / clip.w() * 0.5F + 0.5F) * width;
        float screenY = (clip.y() / clip.w() * 0.5F + 0.5F) * height;
        float deltaX = screenX - width * 0.5F;
        float deltaY = screenY - height * 0.5F;
        return deltaX * deltaX + deltaY * deltaY <= radius * radius;
    }

    private static Vector4f project(
            Vec3 target,
            Camera camera,
            Quaternionf inverseCameraRotation,
            Matrix4f projection
    ) {
        Vec3 relative = target.subtract(camera.getPosition());
        Vector3f viewSpace = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z)
                .rotate(inverseCameraRotation);
        return new Vector4f(viewSpace, 1.0F).mul(projection);
    }

    private static void upload1f(int location, float value) {
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private static void upload2f(int location, float x, float y) {
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    private static boolean isIrisShadowPass() {
        if (!irisShadowMethodResolved) {
            irisShadowMethodResolved = true;
            try {
                Class<?> shadowState = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
                irisShadowMethod = shadowState.getMethod("areShadowsCurrentlyBeingRendered");
            } catch (ReflectiveOperationException ignored) {
                irisShadowMethod = null;
            }
        }

        if (irisShadowMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(irisShadowMethod.invoke(null));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void markPatched() {
        terrainShaderPatched = true;
        if (!loggedPatch) {
            loggedPatch = true;
            RTColony.LOGGER.info("Enabled the fixed RTS terrain focus lens");
        }
    }

    private record LensUniforms(float centerX, float centerY, float radius, float feather, float targetDepth) {
    }

    private record UniformLocations(
            int active,
            int center,
            int radius,
            int feather,
            int targetDepth,
            int strength
    ) {
        private static UniformLocations find(int program) {
            return new UniformLocations(
                    GL20.glGetUniformLocation(program, MARKER),
                    GL20.glGetUniformLocation(program, "rtcolony_LensCenter"),
                    GL20.glGetUniformLocation(program, "rtcolony_LensRadius"),
                    GL20.glGetUniformLocation(program, "rtcolony_LensFeather"),
                    GL20.glGetUniformLocation(program, "rtcolony_LensTargetDepth"),
                    GL20.glGetUniformLocation(program, "rtcolony_LensStrength")
            );
        }
    }
}
