package net.trueog.antipierayog.math;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class FastRayCast {

    // The block access provider.
    public interface BlockAccess {

        // Get the block state at the given position.
        BlockState get(long x, long y, long z);

        // Check if the block at the given position is solid.
        boolean isOpaque(long x, long y, long z);

    }

    // Block access implementation utilizing the NMS server-level directly.
    // TODO: Optimize more!
    static final class NmsBlockAccess implements BlockAccess {

        NmsBlockAccess(ServerLevel level) {

            this.level = level;

        }

        private final ServerLevel level;

        @Override
        public BlockState get(long x, long y, long z) {

            // Get chunk.
            LevelChunk chunk = level.getChunk((int) (x >> 4), (int) (z >> 4));

            return chunk.getBlockState((int) (x % 16), (int) y, (int) (z % 16));

        }

        @Override
        public boolean isOpaque(long x, long y, long z) {

            // Get chunk.
            LevelChunk chunk = level.getChunk((int) (x >> 4), (int) (z >> 4));

            return chunk.getBlockState((int) (x % 16), (int) y, (int) (z % 16)).isOpaque();

        }

    }

    public static BlockAccess blockAccessOf(ServerLevel world) {

        return new NmsBlockAccess(world);

    }

    public static final double STEP_SIZE_OPT_THRESHOLD = 20;
    public static final double STEP_SIZE_OPT_THRESHOLD_SQR = STEP_SIZE_OPT_THRESHOLD * STEP_SIZE_OPT_THRESHOLD;

    /**
     * Checks whether position B is visible from position A, the origin.
     *
     * @param va          Location A.
     * @param vb          Location B.
     * @param blockAccess The block access provider to get the block states in
     *                    certain positions.
     * @return If A can see B.
     */
    public static boolean checkVisibilityOfPositionFromOrigin(Vec3 va, Vec3 vb, BlockAccess blockAccess) {

        // Separate components of A and B and order correctly, as certain components of
        // B might be smaller than A, while the other is expected.
        double ax = va.x;
        double ay = va.y;
        double az = va.z;
        double bx = vb.x;
        double by = vb.y;
        double bz = vb.z;

        /* order */
        if (ax > bx) {

            double t = ax;

            ax = bx;
            bx = t;

        }

        if (ay > by) {

            double t = ay;

            ay = by;
            by = t;

        }

        if (az > bz) {

            double t = az;

            az = bz;
            bz = t;

        }

        // Calculate deltas, and normalize to get base step quotients.
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;

        double mq = 1 / Math.max(dx, Math.max(dy, dz));

        double qx;
        double qy;

        double qz = dz * mq;

        // TODO: Fix this optimization.
        // Calculate largest step size if the distance is worth optimizing
        // using sqr distance for performance
        // double dsq = dx * dx + dy * dy + dz * dz;
        // if (dsq > STEP_SIZE_OPT_THRESHOLD_SQR) {
        double n = Math.max(dx, dy);
        qx = dx / n;
        qy = dy / n;
        // }

        // Perform ray cast.
        double cx = ax;
        double cy = ay;
        double cz = az;
        while (true) {

            long lcx = (long) cx;
            long lcy = (long) cy;
            long lcz = (long) cz;

            // Check if solid.
            if (blockAccess.isOpaque(lcx, lcy, lcz)) {

                // Cannot see.
                return false;

            }

            // Check if at B.
            if (cx >= bx || cy >= by || cz >= bz) {

                return true;

            }

            // Increment position.
            cx += qx;
            cy += qy;
            cz += qz;

        }

    }

}