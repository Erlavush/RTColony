package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/*
 * Read-only optional MineColonies adapter.
 *
 * Based on the reference APIs:
 * - EntityCitizen#getCitizenDataView()
 * - ICitizenDataView
 * - IColonyManager#getBuildingView(...)
 * - IBuildingView
 *
 * Reflection keeps RTColony from requiring MineColonies on the compile classpath.
 */
final class RtsMineColoniesIntegration {
    private static final String COLONY_MANAGER_CLASS = "com.minecolonies.api.colony.IColonyManager";
    private static final String CITIZEN_WINDOW_CLASS = "com.minecolonies.core.client.gui.citizen.MainWindowCitizen";
    private static final long BUILDING_CACHE_TICKS = 20L;

    private static Level cachedBuildingLevel;
    private static long nextBuildingCacheTick = Long.MIN_VALUE;
    private static List<BuildingTarget> cachedBuildingTargets = List.of();

    private RtsMineColoniesIntegration() {
    }

    static Optional<SelectionInfo> createInfo(Minecraft minecraft, RtsTargetingState.TargetSnapshot target) {
        if (target == null) {
            return Optional.empty();
        }

        if (target.kind() == RtsTargetingState.TargetKind.ENTITY && target.entity() != null) {
            return createCitizenInfo(target.entity());
        }

        if ((target.kind() == RtsTargetingState.TargetKind.BLOCK
                || target.kind() == RtsTargetingState.TargetKind.BUILDING)
                && target.blockPos() != null
                && minecraft.level != null) {
            return createBuildingInfo(minecraft.level, target.blockPos());
        }

        return Optional.empty();
    }

    private static Optional<SelectionInfo> createCitizenInfo(Entity entity) {
        Object citizenView = invokeNoArg(entity, "getCitizenDataView").orElse(null);
        if (citizenView == null) {
            return Optional.empty();
        }

        int citizenId = asInt(invokeNoArg(citizenView, "getId").orElse(null), asInt(invokeNoArg(entity, "getCivilianID").orElse(null), 0));
        int colonyId = asInt(invokeNoArg(citizenView, "getColonyId").orElse(null), 0);
        String citizenName = firstNonBlank(asString(invokeNoArg(citizenView, "getName").orElse(null)), entity.getDisplayName().getString());
        String colonyName = colonyName(entity.level(), colonyId);
        String job = firstNonBlank(asString(invokeNoArg(citizenView, "getJob").orElse(null)), "Unassigned");
        String status = citizenStatus(citizenView);
        String work = blockPosShort(invokeNoArg(citizenView, "getWorkBuilding").orElse(null));
        String home = blockPosShort(invokeNoArg(citizenView, "getHomeBuilding").orElse(null));
        String assignment = !"None".equals(work) ? work : home;

        List<Line> lines = new ArrayList<>();
        lines.add(new Line("Job", job));
        lines.add(new Line("Colony", colonyNameWithId(colonyName, colonyId)));
        lines.add(new Line("Status", status));
        lines.add(new Line("Happy", formatOneDecimal(asDouble(invokeNoArg(citizenView, "getHappiness").orElse(null), 0.0D))));
        lines.add(new Line(!"None".equals(work) ? "Work" : "Home", assignment));

        return Optional.of(new SelectionInfo(SelectionKind.CITIZEN, citizenName + " #" + citizenId, lines));
    }

    private static Optional<SelectionInfo> createBuildingInfo(Level level, BlockPos blockPos) {
        Object buildingView = buildingView(level, blockPos).orElse(null);
        return createBuildingInfo(buildingView);
    }

    private static Optional<SelectionInfo> createBuildingInfo(Object buildingView) {
        if (buildingView == null) {
            return Optional.empty();
        }

        Object colony = invokeNoArg(buildingView, "getColony").orElse(null);
        int colonyId = asInt(invokeNoArg(colony, "getID").orElse(null), 0);
        String colonyName = firstNonBlank(asString(invokeNoArg(colony, "getName").orElse(null)), "Colony");
        String title = readableName(firstNonBlank(asString(invokeNoArg(buildingView, "getBuildingDisplayName").orElse(null)), "MineColonies Building"));
        int levelNow = asInt(invokeNoArg(buildingView, "getBuildingLevel").orElse(null), 0);
        int levelMax = asInt(invokeNoArg(buildingView, "getBuildingMaxLevel").orElse(null), 0);
        int assigned = collectionSize(invokeNoArg(buildingView, "getAllAssignedCitizens").orElse(null));
        int requests = collectionSize(invokeNoArg(buildingView, "getOpenRequestsOfBuilding").orElse(null));

        List<Line> lines = new ArrayList<>();
        lines.add(new Line("Colony", colonyNameWithId(colonyName, colonyId)));
        lines.add(new Line("Level", levelNow + "/" + levelMax));
        lines.add(new Line("State", buildingState(buildingView)));
        lines.add(new Line("Workers", Integer.toString(Math.max(0, assigned))));
        lines.add(new Line("Requests", Integer.toString(Math.max(0, requests))));

        return Optional.of(new SelectionInfo(SelectionKind.BUILDING, title, lines));
    }

    static Optional<BuildingTarget> findBuildingAt(Level level, BlockPos blockPos) {
        if (level == null || blockPos == null) {
            return Optional.empty();
        }

        refreshBuildingTargets(level);
        double x = blockPos.getX() + 0.5D;
        double y = blockPos.getY() + 0.5D;
        double z = blockPos.getZ() + 0.5D;
        return cachedBuildingTargets.stream()
                .filter(target -> target.bounds().contains(x, y, z))
                .min(Comparator.comparingDouble(target -> target.bounds().getXsize()
                        * target.bounds().getYsize()
                        * target.bounds().getZsize()));
    }

    static boolean openNativeDetails(Minecraft minecraft, RtsTargetingState.TargetSnapshot target) {
        if (minecraft == null || minecraft.level == null || target == null) {
            return false;
        }

        if (target.kind() == RtsTargetingState.TargetKind.ENTITY && target.entity() != null) {
            Object citizenView = invokeNoArg(target.entity(), "getCitizenDataView").orElse(null);
            if (citizenView == null) {
                return false;
            }

            try {
                Class<?> windowClass = Class.forName(CITIZEN_WINDOW_CLASS);
                for (Constructor<?> constructor : windowClass.getConstructors()) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (parameterTypes.length == 1 && parameterTypes[0].isInstance(citizenView)) {
                        Object window = constructor.newInstance(citizenView);
                        window.getClass().getMethod("open").invoke(window);
                        return true;
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
                return false;
            }
            return false;
        }

        if (target.kind() == RtsTargetingState.TargetKind.BUILDING && target.blockPos() != null) {
            Object buildingView = buildingView(minecraft.level, target.blockPos()).orElse(null);
            if (buildingView == null) {
                return false;
            }
            try {
                Method method = buildingView.getClass().getMethod("openGui", boolean.class);
                method.invoke(buildingView, false);
                return true;
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
                return false;
            }
        }

        return false;
    }

    static void clearCache() {
        cachedBuildingLevel = null;
        nextBuildingCacheTick = Long.MIN_VALUE;
        cachedBuildingTargets = List.of();
    }

    private static void refreshBuildingTargets(Level level) {
        long gameTime = level.getGameTime();
        if (cachedBuildingLevel == level && gameTime < nextBuildingCacheTick) {
            return;
        }

        cachedBuildingLevel = level;
        nextBuildingCacheTick = gameTime + BUILDING_CACHE_TICKS;
        List<BuildingTarget> targets = new ArrayList<>();
        Object manager = colonyManager().orElse(null);
        if (manager == null) {
            cachedBuildingTargets = List.of();
            return;
        }

        try {
            Method coloniesMethod = manager.getClass().getMethod("getColonyViews", Level.class);
            Object coloniesValue = coloniesMethod.invoke(manager, level);
            if (!(coloniesValue instanceof Collection<?> colonies)) {
                cachedBuildingTargets = List.of();
                return;
            }

            for (Object colony : colonies) {
                Object buildingManager = invokeNoArg(colony, "getClientBuildingManager").orElse(null);
                Object buildingsValue = invokeNoArg(buildingManager, "getBuildings").orElse(null);
                if (!(buildingsValue instanceof Map<?, ?> buildings)) {
                    continue;
                }

                for (Object buildingView : buildings.values()) {
                    BlockPos anchor = asBlockPos(invokeNoArg(buildingView, "getPosition").orElse(null));
                    if (anchor == null) {
                        anchor = asBlockPos(invokeNoArg(buildingView, "getID").orElse(null));
                    }
                    if (anchor == null) {
                        continue;
                    }

                    AABB bounds = buildingBounds(level, anchor);
                    SelectionInfo info = createBuildingInfo(buildingView).orElse(null);
                    if (info != null) {
                        targets.add(new BuildingTarget(anchor.immutable(), bounds, info));
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
            cachedBuildingTargets = List.of();
            return;
        }

        cachedBuildingTargets = List.copyOf(targets);
    }

    private static AABB buildingBounds(Level level, BlockPos anchor) {
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        Object corners = invokeNoArg(blockEntity, "getInWorldCorners").orElse(null);
        BlockPos first = asBlockPos(invokeNoArg(corners, "getA").orElse(null));
        BlockPos second = asBlockPos(invokeNoArg(corners, "getB").orElse(null));
        if (first == null || second == null) {
            return new AABB(anchor);
        }

        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        if (maxX - minX > 255 || maxY - minY > 255 || maxZ - minZ > 255) {
            return new AABB(anchor);
        }
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    private static Optional<Object> buildingView(Level level, BlockPos blockPos) {
        Object manager = colonyManager().orElse(null);
        if (manager == null) {
            return Optional.empty();
        }

        try {
            Method method = manager.getClass().getMethod("getBuildingView", ResourceKey.class, BlockPos.class);
            return Optional.ofNullable(method.invoke(manager, level.dimension(), blockPos));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> colonyView(Level level, int colonyId) {
        if (colonyId <= 0) {
            return Optional.empty();
        }

        Object manager = colonyManager().orElse(null);
        if (manager == null) {
            return Optional.empty();
        }

        try {
            Method method = manager.getClass().getMethod("getColonyView", int.class, ResourceKey.class);
            return Optional.ofNullable(method.invoke(manager, colonyId, level.dimension()));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> colonyManager() {
        try {
            Class<?> managerClass = Class.forName(COLONY_MANAGER_CLASS);
            Method method = managerClass.getMethod("getInstance");
            return Optional.ofNullable(method.invoke(null));
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static String citizenStatus(Object citizenView) {
        if (asBoolean(invokeNoArg(citizenView, "isPaused").orElse(null), false)) {
            return "Paused";
        }
        if (asBoolean(invokeNoArg(citizenView, "isChild").orElse(null), false)) {
            return "Child";
        }
        Object visibleStatus = invokeNoArg(citizenView, "getVisibleStatus").orElse(null);
        String status = readableName(asString(visibleStatus));
        if (status.isBlank() || "None".equalsIgnoreCase(status)) {
            return "Working";
        }
        return status;
    }

    private static String buildingState(Object buildingView) {
        if (asBoolean(invokeNoArg(buildingView, "isDeconstructing").orElse(null), false)) {
            return "Deconstructing";
        }
        if (asBoolean(invokeNoArg(buildingView, "isRepairing").orElse(null), false)) {
            return "Repairing";
        }
        if (asBoolean(invokeNoArg(buildingView, "isBuilding").orElse(null), false)) {
            return "Building";
        }
        if (asBoolean(invokeNoArg(buildingView, "hasWorkOrder").orElse(null), false)) {
            return "Work Order";
        }
        return "Ready";
    }

    private static String colonyName(Level level, int colonyId) {
        if (colonyId <= 0) {
            return "No colony";
        }

        Object colony = colonyView(level, colonyId).orElse(null);
        return firstNonBlank(asString(invokeNoArg(colony, "getName").orElse(null)), "Colony");
    }

    private static String colonyNameWithId(String colonyName, int colonyId) {
        if (colonyId <= 0) {
            return colonyName;
        }
        return colonyName + " #" + colonyId;
    }

    private static Optional<Object> invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return Optional.ofNullable(method.invoke(target));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static BlockPos asBlockPos(Object value) {
        return value instanceof BlockPos blockPos ? blockPos : null;
    }

    private static int collectionSize(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Set<?> set) {
            return set.size();
        }
        return -1;
    }

    private static String blockPosShort(Object value) {
        if (value instanceof BlockPos blockPos) {
            return blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
        }
        return "None";
    }

    private static String readableName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lastPath = value;
        int dot = lastPath.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < lastPath.length()) {
            lastPath = lastPath.substring(dot + 1);
        }
        int colon = lastPath.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < lastPath.length()) {
            lastPath = lastPath.substring(colon + 1);
        }

        String[] words = lastPath.replace('_', ' ').replace('-', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase());
            }
        }
        return builder.toString();
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    private static String formatOneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    record SelectionInfo(SelectionKind kind, String title, List<Line> lines) {
    }

    record BuildingTarget(BlockPos anchor, AABB bounds, SelectionInfo info) {
    }

    record Line(String label, String value) {
    }

    enum SelectionKind {
        CITIZEN,
        BUILDING
    }
}
