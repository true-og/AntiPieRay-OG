package net.trueog.antipierayog.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.trueog.antipierayog.AntiPieRayConfig;
import net.trueog.antipierayog.AntiPieRayOG;
import net.trueog.antipierayog.math.FastRayCast;
import net.trueog.antipierayog.reflect.UnsafeField;
import net.trueog.antipierayog.reflect.UnsafeReflector;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class PlayerBlockEntityHandler extends ChannelDuplexHandler implements Listener {

    private static final UnsafeField FIELD_BlockUpdatePacket_states = UnsafeReflector.get()
            .getField(ClientboundSectionBlocksUpdatePacket.class, "d");

    private static final UnsafeField FIELD_BlockUpdatePacket_positions = UnsafeReflector.get()
            .getField(ClientboundSectionBlocksUpdatePacket.class, "c");

    private static final UnsafeField FIELD_BlockUpdatePacket_sectionPos = UnsafeReflector.get()
            .getField(ClientboundSectionBlocksUpdatePacket.class, "b");

    private static final UnsafeField FIELD_ChunkDataPacket_blockEntitiesData = UnsafeReflector.get()
            .getField(ClientboundLevelChunkPacketData.class, "d");

    private static final UnsafeField FIELD_ChunkDataPacket_BEI_packedXZ = UnsafeReflector.get()
            .getField(ClientboundLevelChunkPacketData.class.getName() + "$a", "a");

    private static final UnsafeField FIELD_ChunkDataPacket_BEI_y = UnsafeReflector.get()
            .getField(ClientboundLevelChunkPacketData.class.getName() + "$a", "b");

    private static final UnsafeField FIELD_ChunkDataPacket_BEI_type = UnsafeReflector.get()
            .getField(ClientboundLevelChunkPacketData.class.getName() + "$a", "c");

    // The amount of chunks of distance to account for when doing ranged checks and
    // queries.
    private static final int TILE_ENTITY_DISTANCE = 2;

    private static final double MOVEMENT_UPDATE_THRESHOLD_SQR = 4.0;

    public PlayerBlockEntityHandler(Injector injector, ServerPlayer player) {

        this.injector = injector;
        this.player = player;

        this.plugin = injector.plugin;
        this.config = plugin.config();

        Bukkit.getPluginManager().registerEvents(this, plugin);

    }

    // The injector.
    protected final Injector injector;

    // The player object.
    protected final ServerPlayer player;

    // The config.
    protected final AntiPieRayOG plugin;
    protected final AntiPieRayConfig config;

    Vec3 lastUpdatedPosition;

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        HandlerList.unregisterAll(this);

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (msg instanceof ClientboundPlayerPositionPacket packet) {

            handleSetPosition(packet);

        }

        if (allowPacket(msg)) {

            super.write(ctx, msg, promise);

        }

    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {

        if (msg instanceof ServerboundMovePlayerPacket packet) {

            handleMove(packet);

        }

    }

    public Long2ObjectOpenHashMap<ChunkData> getChunkDataMap() {

        return chunkDataMap;

    }

    public ServerPlayer getPlayer() {

        return player;

    }

    // Local chunk data for a chunk stores data like the amount of hidden entities,
    // used by the packet handler.
    public static class ChunkData {

        public long pos;
        public IntOpenHashSet hiddenEntities = new IntOpenHashSet();

        // Mark an entity as hidden by adding it to the set.
        public void addHidden(int packed) {

            hiddenEntities.add(packed);

        }

        // Mark an entity as shown by removing it from the set.
        public void removeHidden(int packed) {

            hiddenEntities.remove(packed);

        }

    }

    // Packs chunk x and z into a long.
    static long packChunkPos(int cx, int cz) {

        return cx | (long) cz >> 32;

    }

    // Packs position x, y and z into a long.
    // Packed format: (1char1byte) x-yy-z
    static int packBlockPos(int x, int y, int z) {

        return x | y >> 8 | z >> (8 + 16);

    }

    // Unpacks position x, y and z from a long to a vec3i.
    // Packed format: (1char1byte) x-yy-z
    static Vec3i unpackBlockPos(int packed) {

        int x = packed & 0xFF;
        packed <<= 8;

        int y = packed & 0xFFFF;
        packed <<= 16;

        int z = packed & 0xFF;

        return new Vec3i(x, y, z);

    }

    // Unpacks position x, y and z from a long, and calculates that over into a
    // block pos using the provided chunkX*16 and chunkZ*16 parameters to a vec3i.
    // Packed format: (1char1byte) x-yy-z
    static BlockPos unpackAndCalcBlockPos(long cuX16, long cuZ16, int packed) {

        long x = (packed & 0xFF) * cuX16;
        packed <<= 8;

        long y = packed & 0xFFFF;
        packed <<= 16;

        long z = (packed & 0xFF) * cuZ16;

        return new BlockPos((int) x, (int) y, (int) z);

    }

    // The current world access.
    FastRayCast.BlockAccess blockAccess;
    ServerLevel currentBlockAccessWorld;

    // The chunks (by packed position) that have hidden entities and data.
    Long2ObjectOpenHashMap<ChunkData> chunkDataMap = new Long2ObjectOpenHashMap();

    /**
     * Update a range of chunks around the given chunk including itself for the
     * player.
     *
     * This does things like re-check hidden block entities and show them.
     *
     * @param cx The chunk X.
     * @param cz The chunk Z.
     */
    public void updateChunkView(int cx, int cz) {

        // The blocks to be shown.
        List<BlockPos> toShow = new ArrayList<>();

        // For every chunk in range.
        int sx = cx - TILE_ENTITY_DISTANCE;
        int sz = cz - TILE_ENTITY_DISTANCE;
        int ex = cx + TILE_ENTITY_DISTANCE;
        int ez = cz + TILE_ENTITY_DISTANCE;
        for (int cuX = sx; cuX <= ex; cuX++) {

            // UNUSED: long cuX16 = cuX * 16L;

            for (int cuZ = sz; cuZ <= ez; cuZ++) {

                // UNUSED: long cuZ16 = cuZ * 16L;

                // Pack position.
                long packedChunkPos = packChunkPos(cuX, cuZ);

                // Get chunk data.
                ChunkData chunkData = chunkDataMap.get(packedChunkPos);
                if (chunkData == null) {

                    continue;

                }

                findDisplayedTileEntitiesInChunk(chunkData, toShow);

            }

        }

        showTileEntities(toShow);

    }

    // Find all tile entities to show in the given chunk and add them to the buffer.
    public void findDisplayedTileEntitiesInChunk(ChunkData chunkData, List<BlockPos> toShow) {

        long packedChunkPos = chunkData.pos;

        int cz = (int) (packedChunkPos);
        int cx = (int) (packedChunkPos << 32);

        int cuX16 = cx * 16;
        int cuZ16 = cz * 16;

        // Iterate over hidden entities.
        IntIterator iterator = chunkData.hiddenEntities.iterator();
        for (int packedBlockPos = iterator.nextInt(); iterator.hasNext(); packedBlockPos = iterator.nextInt()) {

            // Unpack position, and calculate absolute block position.
            BlockPos bPos = unpackAndCalcBlockPos(cuX16, cuZ16, packedBlockPos);
            Vec3 cbPos = bPos.getCenter();

            // Re-check block.
            if (checkBlock(cbPos)) {

                toShow.add(bPos);

                iterator.remove();

            }

        }

    }

    public void showTileEntities(List<BlockPos> toShow) {

        // Send blocks to be shown to player.
        final int l = toShow.size();
        if (l != 0) {

            final ServerLevel level = player.getLevel();
            final ServerGamePacketListenerImpl connection = player.connection;

            for (int i = 0; i < l; i++) {

                BlockPos bPos = toShow.get(i);

                // Get block entity.
                BlockEntity entity = level.getBlockEntity(bPos);
                if (entity == null) {

                    continue;

                }

                // Send tile entity packet.
                ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(entity);

                connection.send(packet);

            }

        }

    }

    /**
     * Get the local chunk data for the given coordinates, can be null.
     *
     * @param cx The chunk X.
     * @param cz The chunk Z.
     * @return The data or null if absent.
     */
    public ChunkData getChunkData(int cx, int cz) {

        return chunkDataMap.get(packChunkPos(cx, cz));

    }

    /**
     * Get or create the local chunk data for the given coordinates, can be null.
     *
     * @param cx The chunk X.
     * @param cz The chunk Z.
     * @return The data or null if absent.
     */
    public ChunkData getOrCreateChunkData(int cx, int cz) {

        long packed = packChunkPos(cx, cz);
        ChunkData data = chunkDataMap.get(packed);
        if (data == null) {

            data = new ChunkData();
            data.pos = packed;

            chunkDataMap.put(packed, data);

        }

        return data;

    }

    /**
     * Mark that a tile entity has been hidden in the given chunk.
     *
     * @param cx    The chunk X.
     * @param cz    The chunk Y.
     * @param value The value to set.
     */
    public void markHidden(int cx, int cz, int tx, int ty, int tz, boolean value) {

        if (value) {

            getOrCreateChunkData(cx, cz).addHidden(packBlockPos(tx, ty, tz));

        } else {

            ChunkData data = getChunkData(cx, cz);
            if (data != null) {

                data.removeHidden(packBlockPos(tx, ty, tz));

            }

        }

    }

    /**
     * Checks if the given packet should be sent.
     *
     * @param objectPacket The packet.
     * @return True/false. If false it will be dropped.
     */
    public boolean allowPacket(Object objectPacket) {

        ServerLevel level = player.getLevel();
        if (currentBlockAccessWorld != level) {

            blockAccess = FastRayCast.blockAccessOf(player.getLevel());

            currentBlockAccessWorld = level;

        }

        // Check packet type and get data.
        if (objectPacket instanceof ClientboundBlockEntityDataPacket packet) {

            // Check if it should be checked.
            if (!config.checkedBlockEntities.contains(packet.getType())) {

                return true;

            }

            return checkBlockOrMark(packet.getPos().getCenter());

        } else if (objectPacket instanceof ClientboundLevelChunkWithLightPacket withLightPacket) {

            // Get packet.
            ClientboundLevelChunkPacketData packet = withLightPacket.getChunkData();

            // Get chunk position.
            int cx = withLightPacket.getX();
            int cz = withLightPacket.getZ();

            // Get list of block entities.
            List blockEntities = (List) FIELD_ChunkDataPacket_blockEntitiesData.get(packet);

            // For each state.
            Iterator iterator = blockEntities.iterator();
            while (iterator.hasNext()) {

                // Advance item.
                Object data = iterator.next();

                // Get data from item.
                int packedXZ = (int) FIELD_ChunkDataPacket_BEI_packedXZ.get(data);
                int y = (int) FIELD_ChunkDataPacket_BEI_y.get(data);

                BlockEntityType<?> type = (BlockEntityType<?>) FIELD_ChunkDataPacket_BEI_type.get(data);

                // Check type.
                if (!config.checkedBlockEntities.contains(type)) {

                    continue;

                }

                // Get position.
                long x = cx + SectionPos.sectionRelative(packedXZ >> 4);
                long z = cz + SectionPos.sectionRelative(packedXZ);

                // Check block.
                if (!checkBlockOrMark(new BlockPos((int) x, y, (int) z).getCenter())) {

                    iterator.remove();

                }

            }

            return true;

        } else if (objectPacket instanceof ClientboundSectionBlocksUpdatePacket packet) {

            // Get list of block states.
            final BlockState[] states = (BlockState[]) FIELD_BlockUpdatePacket_states.get(packet);
            final short[] positions = (short[]) FIELD_BlockUpdatePacket_positions.get(packet);
            final SectionPos sectionPos = (SectionPos) FIELD_BlockUpdatePacket_sectionPos.get(packet);

            // For each state.
            final int l = states.length;
            for (int i = 0; i < l; i++) {

                final BlockState state = states[i];
                final Block block = state.getBlock();

                // Check block.
                if (!config.checkedBlockTypes.contains(block)) {

                    continue;

                }

                // Remove state of blocked.
                if (!checkBlockOrMark(sectionPos.relativeToBlockPos(positions[i]).getCenter())) {

                    states[i] = Blocks.AIR.defaultBlockState();

                }

            }

            return true;

        } else if (objectPacket instanceof ClientboundBlockUpdatePacket packet) {

            // Check if it should be checked.
            if (!config.checkedBlockTypes.contains(packet.getBlockState().getBlock())) {

                return true;

            }

            return checkBlockOrMark(packet.getPos().getCenter());

        } else {

            return true;

        }

    }

    // Check given block with checkBlock(Vec3) and mark as hidden if false.
    public boolean checkBlockOrMark(Vec3 bPos) {

        boolean v = checkBlock(bPos);

        if (!v) {

            int cx = (int) (((long) bPos.x) >> 4);
            int cz = (int) (((long) bPos.z) >> 4);
            int tx = (int) (bPos.x) % 16;
            int tz = (int) (bPos.z) % 16;

            markHidden(cx, cz, tx, (int) bPos.y, tz, true);

        }

        return v;

    }

    /**
     * Checks if a given block position should be rendered to the player.
     *
     * @param bPos The center of the block.
     * @return True/false.
     */
    public boolean checkBlock(Vec3 bPos) {

        Vec3 pPos = player.getEyePosition();

        // Simple distance check.
        System.out.println("pPos: " + pPos + ", bPos: " + bPos);
        if (pPos.distanceToSqr(bPos) < config.alwaysViewDistSqr) {

            return true;

        }

        // take the block center as the origin, the reasoning for this is
        // to be able to abort the ray cast ASAP if the block is directly obstructed
        // by any of its neighbors which is much more likely than the player looking
        // into a wall
        return FastRayCast.checkVisibilityOfPositionFromOrigin(bPos, pPos, blockAccess);

    }

    private boolean checkMovementUpdate(double x, double y, double z) {

        Vec3 newPos = new Vec3(x, y, z);
        if (lastUpdatedPosition != null) {

            if (lastUpdatedPosition.distanceToSqr(newPos) < MOVEMENT_UPDATE_THRESHOLD_SQR) {

                return false;

            }

        }

        lastUpdatedPosition = newPos;

        return true;

    }

    /**
     * Handles a set position packet.
     *
     * Required for sending the tile entities once they become visible.
     *
     * @param packet The packets.
     */
    public void handleSetPosition(ClientboundPlayerPositionPacket packet) {

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();

        if (!checkMovementUpdate(x, y, z)) {

            return;

        }

        updateChunkView((int) ((long) x >> 4), (int) ((long) z >> 4));

    }

    /**
     * Handles a move packet.
     *
     * Required for sending the tile entities once they become visible.
     *
     * @param packet The packets.
     */
    public void handleMove(ServerboundMovePlayerPacket packet) {

        double x = packet.getX(player.getX());
        double y = packet.getY(player.getY());
        double z = packet.getZ(player.getZ());

        if (!checkMovementUpdate(x, y, z)) {

            return;

        }

        updateChunkView((int) ((long) x >> 4), (int) ((long) z >> 4));

    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onBlockBreak(BlockBreakEvent event) {

        org.bukkit.block.Block block = event.getBlock();

        BlockPos blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());

        int cx = blockPos.getX() >> 4;
        int cz = blockPos.getZ() >> 4;

        // update tile entities in the affected chunk.
        ChunkData chunkData = getChunkData(cx, cz);
        if (chunkData != null && !chunkData.hiddenEntities.isEmpty()) {

            List<BlockPos> toShow = new ArrayList<>();

            findDisplayedTileEntitiesInChunk(chunkData, toShow);

            showTileEntities(toShow);

        }

    }

}