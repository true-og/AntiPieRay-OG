package net.trueog.antiPieRayOG

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.BlockIterator
import org.bukkit.util.Vector

class BlockEntityHider {
    companion object {
        val eyeOffsetMap =
            mapOf(
                // Up
                Vector(0, 1, 0) to
                    listOf(
                        Vector(0.5, 0.5, 0.5),
                        Vector(0.5, 0.5, -0.5),
                        Vector(-0.5, 0.5, 0.5),
                        Vector(-0.5, 0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // Down
                Vector(0, -1, 0) to
                    listOf(
                        Vector(0.5, -0.5, 0.5),
                        Vector(0.5, -0.5, -0.5),
                        Vector(-0.5, -0.5, 0.5),
                        Vector(-0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // North
                Vector(0, 0, -1) to
                    listOf(
                        Vector(0.5, 0.5, -0.5),
                        Vector(0.5, -0.5, -0.5),
                        Vector(-0.5, 0.5, -0.5),
                        Vector(-0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // South
                Vector(0, 0, 1) to
                    listOf(
                        Vector(0.5, 0.5, 0.5),
                        Vector(0.5, -0.5, 0.5),
                        Vector(-0.5, 0.5, 0.5),
                        Vector(-0.5, -0.5, 0.5),
                        Vector(0, 0, 0),
                    ),
                // West
                Vector(-1, 0, 0) to
                    listOf(
                        Vector(-0.5, 0.5, 0.5),
                        Vector(-0.5, -0.5, 0.5),
                        Vector(-0.5, 0.5, -0.5),
                        Vector(-0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // East
                Vector(1, 0, 0) to
                    listOf(
                        Vector(0.5, 0.5, 0.5),
                        Vector(0.5, -0.5, 0.5),
                        Vector(0.5, 0.5, -0.5),
                        Vector(0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
            )

        val blockOffsets =
            listOf(
                Vector(0.5, 0.5, 0.5),
                Vector(0.5, 0.5, -0.5),
                Vector(0.5, -0.5, 0.5),
                Vector(0.5, -0.5, -0.5),
                Vector(-0.5, 0.5, 0.5),
                Vector(-0.5, 0.5, -0.5),
                Vector(-0.5, -0.5, 0.5),
                Vector(-0.5, -0.5, -0.5),
            )

        fun canSee(eye: Location, loc: Location): Boolean {
            // Get the center
            val blockCenterLoc = loc.clone().add(0.5, 0.5, 0.5)
            if (blockCenterLoc.toVector().subtract(eye.toVector()).dot(eye.direction) < 0) return false

            val blockCenterDir = eye.clone().toVector().subtract(blockCenterLoc.toVector()).normalize()

            var bestIndex: Int = -1
            var bestDot = -Double.MAX_VALUE
            eyeOffsetMap.entries.forEachIndexed { index, (faceVec, _) ->
                val dot = blockCenterDir.dot(faceVec)
                if (dot > bestDot) {
                    bestDot = dot
                    bestIndex = index
                }
            }
            val eyeOffsets = eyeOffsetMap.values.elementAt(bestIndex)

            for (eyeOffset in eyeOffsets) {
                val offsetEye = eye.clone().add(eyeOffset)
                for (offset in blockOffsets) {
                    val blockLoc = blockCenterLoc.clone().add(offset)
                    val direction = blockLoc.toVector().subtract(offsetEye.toVector()).normalize()
                    val eyeFacing = offsetEye.clone().setDirection(direction)
                    val maxDistance = eyeFacing.distance(blockLoc).roundToInt() - 1
                    val blockIterator = BlockIterator(eyeFacing, 0.0, maxDistance)

                    var visible = true
                    while (blockIterator.hasNext()) {
                        val hitBlock = blockIterator.next()
                        if (hitBlock == loc.block) {
                            break
                        }
                        if (!isAir(hitBlock.type) && !isLiquid(hitBlock.type) && !isTransparent(hitBlock.type)) {
                            visible = false
                            break
                        }
                    }

                    if (visible) return true
                }
            }
            return false
        }

        fun isTransparent(material: Material): Boolean {
            return when (material) {
                // <editor-fold desc="Transparent materials">
                Material.ACACIA_DOOR,
                Material.ACACIA_FENCE,
                Material.ACACIA_FENCE_GATE,
                //                Material.ACACIA_HANGING_SIGN,
                Material.ACACIA_LEAVES,
                Material.ACACIA_LOG,
                Material.ACACIA_PLANKS,
                Material.ACACIA_PRESSURE_PLATE,
                Material.ACACIA_SIGN,
                Material.ACACIA_SLAB,
                Material.ACACIA_STAIRS,
                Material.ACACIA_TRAPDOOR,
                //                Material.ACACIA_WALL_HANGING_SIGN,
                Material.ACACIA_WALL_SIGN,
                Material.ACACIA_WOOD,
                Material.AZALEA_LEAVES,
                Material.BAMBOO,
                //                Material.BAMBOO_BLOCK,
                //                Material.BAMBOO_DOOR,
                //                Material.BAMBOO_FENCE,
                //                Material.BAMBOO_FENCE_GATE,
                //                Material.BAMBOO_HANGING_SIGN,
                //                Material.BAMBOO_MOSAIC,
                //                Material.BAMBOO_MOSAIC_SLAB,
                //                Material.BAMBOO_MOSAIC_STAIRS,
                //                Material.BAMBOO_PLANKS,
                //                Material.BAMBOO_PRESSURE_PLATE,
                Material.BAMBOO_SAPLING,
                //                Material.BAMBOO_SIGN,
                //                Material.BAMBOO_SLAB,
                //                Material.BAMBOO_STAIRS,
                //                Material.BAMBOO_TRAPDOOR,
                //                Material.BAMBOO_WALL_HANGING_SIGN,
                //                Material.BAMBOO_WALL_SIGN,
                Material.BARREL,
                Material.BEEHIVE,
                Material.BEE_NEST,
                Material.BIRCH_DOOR,
                Material.BIRCH_FENCE,
                Material.BIRCH_FENCE_GATE,
                //                Material.BIRCH_HANGING_SIGN,
                Material.BIRCH_LEAVES,
                Material.BIRCH_LOG,
                Material.BIRCH_PLANKS,
                Material.BIRCH_PRESSURE_PLATE,
                Material.BIRCH_SIGN,
                Material.BIRCH_SLAB,
                Material.BIRCH_STAIRS,
                Material.BIRCH_TRAPDOOR,
                Material.BIRCH_WALL_SIGN,
                Material.BIRCH_WOOD,
                Material.BLACK_BANNER,
                Material.BLACK_BED,
                Material.BLACK_CARPET,
                Material.BLACK_WALL_BANNER,
                Material.BLACK_WOOL,
                Material.BLUE_BANNER,
                Material.BLUE_BED,
                Material.BLUE_CARPET,
                Material.BLUE_WALL_BANNER,
                Material.BLUE_WOOL,
                Material.BOOKSHELF,
                Material.BROWN_BANNER,
                Material.BROWN_BED,
                Material.BROWN_CARPET,
                Material.BROWN_MUSHROOM_BLOCK,
                Material.BROWN_WALL_BANNER,
                Material.BROWN_WOOL,
                Material.CAMPFIRE,
                Material.CARTOGRAPHY_TABLE,
                //                Material.CHERRY_DOOR,
                //                Material.CHERRY_FENCE,
                //                Material.CHERRY_FENCE_GATE,
                //                Material.CHERRY_HANGING_SIGN,
                //                Material.CHERRY_LEAVES,
                //                Material.CHERRY_LOG,
                //                Material.CHERRY_PLANKS,
                //                Material.CHERRY_PRESSURE_PLATE,
                //                Material.CHERRY_SIGN,
                //                Material.CHERRY_SLAB,
                //                Material.CHERRY_STAIRS,
                //                Material.CHERRY_TRAPDOOR,
                //                Material.CHERRY_WALL_HANGING_SIGN,
                //                Material.CHERRY_WALL_SIGN,
                //                Material.CHERRY_WOOD,
                Material.CHEST,
                //                Material.CHISELED_BOOKSHELF,
                Material.COMPOSTER,
                Material.CRAFTING_TABLE,
                //                Material.CRIMSON_HANGING_SIGN,
                //                Material.CRIMSON_WALL_HANGING_SIGN,
                Material.CYAN_BANNER,
                Material.CYAN_BED,
                Material.CYAN_CARPET,
                Material.CYAN_WALL_BANNER,
                Material.CYAN_WOOL,
                Material.DARK_OAK_DOOR,
                Material.DARK_OAK_FENCE,
                Material.DARK_OAK_FENCE_GATE,
                //                Material.DARK_OAK_HANGING_SIGN,
                Material.DARK_OAK_LEAVES,
                Material.DARK_OAK_LOG,
                Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_PRESSURE_PLATE,
                Material.DARK_OAK_SIGN,
                Material.DARK_OAK_SLAB,
                Material.DARK_OAK_STAIRS,
                Material.DARK_OAK_TRAPDOOR,
                //                Material.DARK_OAK_WALL_HANGING_SIGN,
                Material.DARK_OAK_WALL_SIGN,
                Material.DARK_OAK_WOOD,
                Material.DAYLIGHT_DETECTOR,
                Material.DEAD_BUSH,
                Material.FERN,
                Material.FLETCHING_TABLE,
                Material.FLOWERING_AZALEA_LEAVES,
                Material.GLOW_LICHEN,
                Material.GRASS,
                Material.GRAY_BANNER,
                Material.GRAY_BED,
                Material.GRAY_CARPET,
                Material.GRAY_WALL_BANNER,
                Material.GRAY_WOOL,
                Material.GREEN_BANNER,
                Material.GREEN_BED,
                Material.GREEN_CARPET,
                Material.GREEN_WALL_BANNER,
                Material.GREEN_WOOL,
                Material.HANGING_ROOTS,
                Material.JUKEBOX,
                Material.JUNGLE_DOOR,
                Material.JUNGLE_FENCE,
                Material.JUNGLE_FENCE_GATE,
                //                Material.JUNGLE_HANGING_SIGN,
                Material.JUNGLE_LEAVES,
                Material.JUNGLE_LOG,
                Material.JUNGLE_PLANKS,
                Material.JUNGLE_PRESSURE_PLATE,
                Material.JUNGLE_SIGN,
                Material.JUNGLE_SLAB,
                Material.JUNGLE_STAIRS,
                Material.JUNGLE_TRAPDOOR,
                //                Material.JUNGLE_WALL_HANGING_SIGN,
                Material.JUNGLE_WALL_SIGN,
                Material.JUNGLE_WOOD,
                Material.LARGE_FERN,
                Material.LECTERN,
                Material.LIGHT_BLUE_BANNER,
                Material.LIGHT_BLUE_BED,
                Material.LIGHT_BLUE_CARPET,
                Material.LIGHT_BLUE_WALL_BANNER,
                Material.LIGHT_BLUE_WOOL,
                Material.LIGHT_GRAY_BANNER,
                Material.LIGHT_GRAY_BED,
                Material.LIGHT_GRAY_CARPET,
                Material.LIGHT_GRAY_WALL_BANNER,
                Material.LIGHT_GRAY_WOOL,
                Material.LILAC,
                Material.LIME_BANNER,
                Material.LIME_BED,
                Material.LIME_CARPET,
                Material.LIME_WALL_BANNER,
                Material.LIME_WOOL,
                Material.LOOM,
                Material.MAGENTA_BANNER,
                Material.MAGENTA_BED,
                Material.MAGENTA_CARPET,
                Material.MAGENTA_WALL_BANNER,
                Material.MAGENTA_WOOL,
                Material.MANGROVE_DOOR,
                Material.MANGROVE_FENCE,
                Material.MANGROVE_FENCE_GATE,
                //                Material.MANGROVE_HANGING_SIGN,
                Material.MANGROVE_LEAVES,
                Material.MANGROVE_LOG,
                Material.MANGROVE_PLANKS,
                Material.MANGROVE_PRESSURE_PLATE,
                Material.MANGROVE_ROOTS,
                Material.MANGROVE_SIGN,
                Material.MANGROVE_SLAB,
                Material.MANGROVE_STAIRS,
                Material.MANGROVE_TRAPDOOR,
                //                Material.MANGROVE_WALL_HANGING_SIGN,
                Material.MANGROVE_WALL_SIGN,
                Material.MANGROVE_WOOD,
                Material.MUSHROOM_STEM,
                Material.NOTE_BLOCK,
                Material.OAK_DOOR,
                Material.OAK_FENCE,
                Material.OAK_FENCE_GATE,
                //                Material.OAK_HANGING_SIGN,
                Material.OAK_LEAVES,
                Material.OAK_LOG,
                Material.OAK_PLANKS,
                Material.OAK_PRESSURE_PLATE,
                Material.OAK_SIGN,
                Material.OAK_SLAB,
                Material.OAK_STAIRS,
                Material.OAK_TRAPDOOR,
                //                Material.OAK_WALL_HANGING_SIGN,
                Material.OAK_WALL_SIGN,
                Material.OAK_WOOD,
                Material.ORANGE_BANNER,
                Material.ORANGE_BED,
                Material.ORANGE_CARPET,
                Material.ORANGE_WALL_BANNER,
                Material.ORANGE_WOOL,
                Material.PEONY,
                Material.PINK_BANNER,
                Material.PINK_BED,
                Material.PINK_CARPET,
                Material.PINK_WALL_BANNER,
                Material.PINK_WOOL,
                Material.PURPLE_BANNER,
                Material.PURPLE_BED,
                Material.PURPLE_CARPET,
                Material.PURPLE_WALL_BANNER,
                Material.PURPLE_WOOL,
                Material.RED_BANNER,
                Material.RED_BED,
                Material.RED_CARPET,
                Material.RED_MUSHROOM_BLOCK,
                Material.RED_WALL_BANNER,
                Material.RED_WOOL,
                Material.ROSE_BUSH,
                Material.SMITHING_TABLE,
                Material.SOUL_CAMPFIRE,
                Material.SPRUCE_DOOR,
                Material.SPRUCE_FENCE,
                Material.SPRUCE_FENCE_GATE,
                //                Material.SPRUCE_HANGING_SIGN,
                Material.SPRUCE_LEAVES,
                Material.SPRUCE_LOG,
                Material.SPRUCE_PLANKS,
                Material.SPRUCE_PRESSURE_PLATE,
                Material.SPRUCE_SIGN,
                Material.SPRUCE_SLAB,
                Material.SPRUCE_STAIRS,
                Material.SPRUCE_TRAPDOOR,
                //                Material.SPRUCE_WALL_HANGING_SIGN,
                Material.SPRUCE_WALL_SIGN,
                Material.SPRUCE_WOOD,
                Material.STRIPPED_ACACIA_LOG,
                Material.STRIPPED_ACACIA_WOOD,
                //                Material.STRIPPED_BAMBOO_BLOCK,
                Material.STRIPPED_BIRCH_LOG,
                Material.STRIPPED_BIRCH_WOOD,
                //                Material.STRIPPED_CHERRY_LOG,
                //                Material.STRIPPED_CHERRY_WOOD,
                Material.STRIPPED_DARK_OAK_LOG,
                Material.STRIPPED_DARK_OAK_WOOD,
                Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_JUNGLE_WOOD,
                Material.STRIPPED_MANGROVE_LOG,
                Material.STRIPPED_MANGROVE_WOOD,
                Material.STRIPPED_OAK_LOG,
                Material.STRIPPED_OAK_WOOD,
                Material.STRIPPED_SPRUCE_LOG,
                Material.STRIPPED_SPRUCE_WOOD,
                Material.SUNFLOWER,
                Material.TALL_GRASS,
                Material.TNT,
                Material.TRAPPED_CHEST,
                Material.VINE,
                //                Material.WARPED_HANGING_SIGN,
                //                Material.WARPED_WALL_HANGING_SIGN,
                Material.WHITE_BANNER,
                Material.WHITE_BED,
                Material.WHITE_CARPET,
                Material.WHITE_WALL_BANNER,
                Material.WHITE_WOOL,
                Material.YELLOW_BANNER,
                Material.YELLOW_BED,
                Material.YELLOW_CARPET,
                Material.YELLOW_WALL_BANNER,
                Material.YELLOW_WOOL ->
                    // </editor-fold>
                    true
                else -> false
            }
        }

        fun isAir(material: Material): Boolean {
            return when (material) {
                Material.AIR,
                Material.CAVE_AIR,
                Material.VOID_AIR -> true
                else -> false
            }
        }

        fun isLiquid(material: Material): Boolean {
            return when (material) {
                Material.WATER,
                Material.LAVA -> true
                else -> false
            }
        }

        val neighbourOffsets =
            listOf(
                Triple(1, 0, 0),
                Triple(-1, 0, 0),
                Triple(0, 1, 0),
                Triple(0, -1, 0),
                Triple(0, 0, 1),
                Triple(0, 0, -1),
            )

        fun getAdjacentPositions(pos: BlockPosition): Set<BlockPosition> =
            neighbourOffsets.map { (dx, dy, dz) -> pos.add(dx, dy, dz) }.toSet()

        data class BlockPosition(var x: Int, var y: Int, var z: Int) {
            fun add(x: Int, y: Int, z: Int): BlockPosition {
                return BlockPosition(this.x + x, this.y + y, this.z + z)
            }

            fun toLocation(world: World) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        }

        fun Location.toBlockPosition() = BlockPosition(blockX, blockY, blockZ)
    }

    val hiddenBlocksForPlayer: MutableMap<UUID, MutableSet<BlockPosition>> = ConcurrentHashMap()
    val blockHiddenForPlayers: MutableMap<BlockPosition, MutableSet<UUID>> = ConcurrentHashMap()

    fun addPos(uuid: UUID, pos: BlockPosition) {
        hiddenBlocksForPlayer.computeIfAbsent(uuid) { ConcurrentHashMap.newKeySet() }.add(pos)
        blockHiddenForPlayers.computeIfAbsent(pos) { ConcurrentHashMap.newKeySet() }.add(uuid)
    }

    fun removePos(uuid: UUID, pos: BlockPosition) {
        hiddenBlocksForPlayer[uuid]?.remove(pos)
        blockHiddenForPlayers[pos]?.remove(uuid)

        if (hiddenBlocksForPlayer[uuid]?.isEmpty() == true) hiddenBlocksForPlayer.remove(uuid)
        if (blockHiddenForPlayers[pos]?.isEmpty() == true) blockHiddenForPlayers.remove(pos)
    }

    fun removeAllPos(uuid: UUID) {
        val hiddenBlocks = hiddenBlocksForPlayer[uuid] ?: return
        hiddenBlocks.forEach { blockHiddenForPlayers[it]?.remove(uuid) }
        hiddenBlocksForPlayer.remove(uuid)
    }

    fun removeOutOfRangeBlocks(player: Player) {
        val threshold = min(player.viewDistance, Bukkit.getViewDistance()) + 1
        val chunkX = player.location.x.toInt() shr 4
        val chunkZ = player.location.z.toInt() shr 4
        blockHiddenForPlayers
            .filterKeys { abs(chunkX - (it.x shr 4)) > threshold || abs(chunkZ - (it.z shr 4)) > threshold }
            .forEach { (pos, uuids) -> uuids.forEach { uuid -> removePos(uuid, pos) } }
    }

    fun removeIfNeeded(pos: BlockPosition, world: World) {
        if (world.getBlockAt(pos.toLocation(world)).type !in AntiPieRay.hideMaterials) {
            blockHiddenForPlayers
                .filterKeys { pos == it }
                .values
                .forEach { uuids -> uuids.forEach { uuid -> removePos(uuid, pos) } }
        }
    }

    fun updateBlockVisibility(player: Player) {
        val blockList = hiddenBlocksForPlayer[player.uniqueId] ?: return
        blockList.forEach {
            val loc = it.toLocation(player.world)
            val visible = canSee(player.eyeLocation, loc)
            if (!visible) {
                return@forEach
            }
            val block = player.world.getBlockAt(loc)
            removePos(player.uniqueId, it)
            player.sendBlockChange(loc, block.blockData)
        }
    }

    fun updateBlockVisibility(pos: BlockPosition, world: World) {
        removeIfNeeded(pos, world)
        val adjacentPositions = getAdjacentPositions(pos)
        adjacentPositions.forEach { adjacentPos ->
            val loc = adjacentPos.toLocation(world)
            blockHiddenForPlayers[adjacentPos]?.forEach { uuid ->
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                val visible = canSee(player.eyeLocation, loc)
                if (!visible) {
                    return@forEach
                }
                val block = player.world.getBlockAt(loc)
                removePos(player.uniqueId, adjacentPos)
                player.sendBlockChange(loc, block.blockData)
            }
        }
    }
}
