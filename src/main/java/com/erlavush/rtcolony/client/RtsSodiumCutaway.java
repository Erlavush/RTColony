package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * RTColony adaptation of Dungeons Perspective's Sodium block-model cutaway.
 *
 * <p>The original mod cancels selected models while Sodium compiles a chunk,
 * combines a camera-to-character cone with a connected-air flood fill, and
 * asks Sodium to rebuild affected sections. RTColony retains that architecture
 * but centers it on the selected/followed entity and refreshes when any view or
 * blocker input changes.</p>
 */
public final class RtsSodiumCutaway {
    private static final int MAX_VERTICAL_UP = 24;
    private static final int MAX_VERTICAL_DOWN = 8;
    private static final int MAX_FLOOD_RADIUS = 12;
    private static final int MAX_FLOOD_NODES = 40_000;
    private static final double MAX_CYLINDER_RADIUS = 3.75D;
    private static final double CONE_HALF_ANGLE_TAN = 1.0D;
    private static final double GROUND_MARGIN = 0.24D;

    private static volatile MeshSnapshot meshSnapshot = MeshSnapshot.empty();
    private static Fingerprint lastFingerprint;
    private static AABB lastRebuildBounds;
    private static boolean sodiumUnavailableLogged;

    private RtsSodiumCutaway() {
    }

    public static void tick(Minecraft minecraft) {
        RtsCutawayState.Snapshot cutaway = RtsCutawayState.getSnapshot();
        if (minecraft == null
                || minecraft.level == null
                || cutaway == null
                || cutaway.openAmount() <= 0.001F) {
            deactivate();
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            deactivate();
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        Vec3 targetCenter = cutaway.targetCenter();
        int radius = Math.max(2, (int) Math.ceil(MAX_FLOOD_RADIUS * cutaway.openAmount()));
        Fingerprint fingerprint = Fingerprint.create(cutaway, cameraPosition, radius);
        if (fingerprint.equals(lastFingerprint)) {
            return;
        }

        MeshSnapshot previous = meshSnapshot;
        Map<Long, Integer> floodCeilings = buildFloodCeilings(
                minecraft.level,
                BlockPos.containing(targetCenter.x, cutaway.targetBounds().minY + 0.5D, targetCenter.z),
                radius
        );
        Set<Long> blockers = new HashSet<>(cutaway.blockerBlocks().size());
        for (BlockPos blocker : cutaway.blockerBlocks()) {
            blockers.add(blocker.asLong());
        }

        MeshSnapshot next = new MeshSnapshot(
                true,
                Set.copyOf(blockers),
                Map.copyOf(floodCeilings),
                cameraPosition,
                targetCenter,
                cutaway.targetBounds().minY,
                MAX_CYLINDER_RADIUS * cutaway.openAmount()
        );
        meshSnapshot = next;
        lastFingerprint = fingerprint;

        AABB nextBounds = rebuildBounds(cutaway, cameraPosition, radius);
        scheduleRebuild(union(lastRebuildBounds, nextBounds));
        lastRebuildBounds = nextBounds;

        if (!previous.active()) {
            RTColony.LOGGER.info("Enabled Dungeons Perspective-style Sodium mesh cutaway");
        }
    }

    public static void clear() {
        deactivate();
    }

    public static boolean isActive() {
        return meshSnapshot.active();
    }

    /** Called from Sodium's chunk compilation worker threads. */
    public static boolean shouldCull(BlockPos pos, BlockState state) {
        MeshSnapshot snapshot = meshSnapshot;
        if (!snapshot.active() || state.hasBlockEntity()) {
            return false;
        }

        long packed = pos.asLong();
        if (snapshot.blockers().contains(packed)) {
            return true;
        }

        Integer floodCeiling = snapshot.floodCeilings().get(packXZ(pos.getX(), pos.getZ()));
        if (floodCeiling != null && pos.getY() > floodCeiling) {
            return true;
        }

        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof LadderBlock) {
            return false;
        }

        return insideCameraCone(pos, snapshot);
    }

    private static boolean insideCameraCone(BlockPos pos, MeshSnapshot snapshot) {
        double bx = pos.getX() + 0.5D;
        double by = pos.getY() + 0.5D;
        double bz = pos.getZ() + 0.5D;
        if (by <= snapshot.targetFloorY() + GROUND_MARGIN) {
            return false;
        }

        Vec3 axis = snapshot.cameraPosition().subtract(snapshot.targetCenter());
        double axisLengthSquared = axis.lengthSqr();
        if (axisLengthSquared < 1.0E-8D) {
            return false;
        }

        Vec3 toBlock = new Vec3(bx, by, bz).subtract(snapshot.targetCenter());
        double projectionRatio = toBlock.dot(axis) / axisLengthSquared;
        if (projectionRatio <= 0.0D || projectionRatio >= 1.0D) {
            return false;
        }

        Vec3 projected = axis.scale(projectionRatio);
        double perpendicularDistanceSquared = toBlock.subtract(projected).lengthSqr();
        double projectedDistance = Math.sqrt(axisLengthSquared) * projectionRatio;
        double coneRadius = projectedDistance * CONE_HALF_ANGLE_TAN;
        double effectiveRadius = Math.min(coneRadius, snapshot.cylinderRadius());
        return perpendicularDistanceSquared <= effectiveRadius * effectiveRadius;
    }

    private static Map<Long, Integer> buildFloodCeilings(
            ClientLevel level,
            BlockPos requestedStart,
            int radius
    ) {
        BlockPos start = findPassableStart(level, requestedStart);
        if (start == null) {
            return Map.of();
        }

        int originX = start.getX();
        int originZ = start.getZ();
        int minY = start.getY() - MAX_VERTICAL_DOWN;
        int maxY = start.getY() + MAX_VERTICAL_UP;
        int radiusSquared = radius * radius;

        Map<Long, Integer> ceilings = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        visited.add(start.asLong());

        while (!queue.isEmpty() && visited.size() < MAX_FLOOD_NODES) {
            BlockPos current = queue.removeFirst();
            ceilings.merge(packXZ(current.getX(), current.getZ()), current.getY(), Math::max);

            visitNeighbor(level, current.offset(1, 0, 0), originX, originZ, minY, maxY, radiusSquared, visited, queue);
            visitNeighbor(level, current.offset(-1, 0, 0), originX, originZ, minY, maxY, radiusSquared, visited, queue);
            visitNeighbor(level, current.offset(0, 0, 1), originX, originZ, minY, maxY, radiusSquared, visited, queue);
            visitNeighbor(level, current.offset(0, 0, -1), originX, originZ, minY, maxY, radiusSquared, visited, queue);
            visitNeighbor(level, current.above(), originX, originZ, minY, maxY, radiusSquared, visited, queue);
            visitNeighbor(level, current.below(), originX, originZ, minY, maxY, radiusSquared, visited, queue);
        }

        return ceilings;
    }

    private static void visitNeighbor(
            ClientLevel level,
            BlockPos pos,
            int originX,
            int originZ,
            int minY,
            int maxY,
            int radiusSquared,
            Set<Long> visited,
            ArrayDeque<BlockPos> queue
    ) {
        if (pos.getY() < minY || pos.getY() > maxY) {
            return;
        }
        int dx = pos.getX() - originX;
        int dz = pos.getZ() - originZ;
        if (dx * dx + dz * dz > radiusSquared || !level.hasChunkAt(pos)) {
            return;
        }
        long packed = pos.asLong();
        if (!visited.add(packed)) {
            return;
        }
        if (isPassable(level, pos)) {
            queue.addLast(pos.immutable());
        }
    }

    private static BlockPos findPassableStart(ClientLevel level, BlockPos start) {
        for (int offset = 0; offset <= 3; offset++) {
            BlockPos above = start.above(offset);
            if (isPassable(level, above)) {
                return above;
            }
        }
        return null;
    }

    private static boolean isPassable(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    private static AABB rebuildBounds(
            RtsCutawayState.Snapshot cutaway,
            Vec3 cameraPosition,
            int floodRadius
    ) {
        AABB bounds = cutaway.targetBounds().inflate(floodRadius, MAX_VERTICAL_UP, floodRadius);
        bounds = bounds.minmax(new AABB(cameraPosition, cameraPosition).inflate(MAX_CYLINDER_RADIUS + 2.0D));
        for (BlockPos blocker : cutaway.blockerBlocks()) {
            bounds = bounds.minmax(new AABB(blocker).inflate(1.0D));
        }
        return bounds;
    }

    private static AABB union(AABB first, AABB second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.minmax(second);
    }

    private static void deactivate() {
        if (!meshSnapshot.active() && lastRebuildBounds == null) {
            return;
        }
        AABB restoreBounds = lastRebuildBounds;
        meshSnapshot = MeshSnapshot.empty();
        lastFingerprint = null;
        lastRebuildBounds = null;
        scheduleRebuild(restoreBounds);
    }

    private static void scheduleRebuild(AABB bounds) {
        if (bounds == null) {
            return;
        }
        try {
            Class<?> rendererClass = Class.forName(
                    "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer"
            );
            Method instanceNullable = rendererClass.getMethod("instanceNullable");
            Object renderer = instanceNullable.invoke(null);
            if (renderer == null) {
                return;
            }
            Method rebuild = rendererClass.getMethod(
                    "scheduleRebuildForBlockArea",
                    int.class, int.class, int.class,
                    int.class, int.class, int.class,
                    boolean.class
            );
            rebuild.invoke(
                    renderer,
                    (int) Math.floor(bounds.minX),
                    (int) Math.floor(bounds.minY),
                    (int) Math.floor(bounds.minZ),
                    (int) Math.ceil(bounds.maxX),
                    (int) Math.ceil(bounds.maxY),
                    (int) Math.ceil(bounds.maxZ),
                    true
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!sodiumUnavailableLogged) {
                sodiumUnavailableLogged = true;
                RTColony.LOGGER.warn("Sodium mesh cutaway backend is unavailable", exception);
            }
        }
    }

    private static long packXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private record MeshSnapshot(
            boolean active,
            Set<Long> blockers,
            Map<Long, Integer> floodCeilings,
            Vec3 cameraPosition,
            Vec3 targetCenter,
            double targetFloorY,
            double cylinderRadius
    ) {
        private static MeshSnapshot empty() {
            return new MeshSnapshot(false, Set.of(), Map.of(), Vec3.ZERO, Vec3.ZERO, 0.0D, 0.0D);
        }
    }

    private record Fingerprint(
            int entityId,
            int openStep,
            int cameraX,
            int cameraY,
            int cameraZ,
            int targetX,
            int targetY,
            int targetZ,
            int floodRadius,
            List<Long> blockers
    ) {
        private static Fingerprint create(
                RtsCutawayState.Snapshot snapshot,
                Vec3 camera,
                int radius
        ) {
            List<Long> blockers = snapshot.blockerBlocks().stream()
                    .map(BlockPos::asLong)
                    .toList();
            return new Fingerprint(
                    snapshot.entityId(),
                    Math.round(snapshot.openAmount() * 10.0F),
                    quantize(camera.x), quantize(camera.y), quantize(camera.z),
                    quantize(snapshot.targetCenter().x),
                    quantize(snapshot.targetCenter().y),
                    quantize(snapshot.targetCenter().z),
                    radius,
                    blockers
            );
        }

        private static int quantize(double value) {
            return (int) Math.floor(value * 4.0D);
        }
    }
}
