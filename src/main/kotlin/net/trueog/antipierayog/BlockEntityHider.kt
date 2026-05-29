package net.trueog.antipierayog

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld
import org.bukkit.entity.Player
import org.bukkit.util.BlockIterator
import org.bukkit.util.Vector

class BlockEntityHider {
    companion object {
        val eyeOffsetMap =
            mapOf(
                // Up
                Vector(0, 1, 0) to
                    setOf(
                        Vector(0.5, 0.5, 0.5),
                        Vector(0.5, 0.5, -0.5),
                        Vector(-0.5, 0.5, 0.5),
                        Vector(-0.5, 0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // Down
                Vector(0, -1, 0) to
                    setOf(
                        Vector(0.5, -0.5, 0.5),
                        Vector(0.5, -0.5, -0.5),
                        Vector(-0.5, -0.5, 0.5),
                        Vector(-0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // North
                Vector(0, 0, -1) to
                    setOf(
                        Vector(0.5, 0.5, -0.5),
                        Vector(0.5, -0.5, -0.5),
                        Vector(-0.5, 0.5, -0.5),
                        Vector(-0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // South
                Vector(0, 0, 1) to
                    setOf(
                        Vector(0.5, 0.5, 0.5),
                        Vector(0.5, -0.5, 0.5),
                        Vector(-0.5, 0.5, 0.5),
                        Vector(-0.5, -0.5, 0.5),
                        Vector(0, 0, 0),
                    ),
                // West
                Vector(-1, 0, 0) to
                    setOf(
                        Vector(-0.5, 0.5, 0.5),
                        Vector(-0.5, -0.5, 0.5),
                        Vector(-0.5, 0.5, -0.5),
                        Vector(-0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
                // East
                Vector(1, 0, 0) to
                    setOf(
                        Vector(0.5, 0.5, 0.5),
                        Vector(0.5, -0.5, 0.5),
                        Vector(0.5, 0.5, -0.5),
                        Vector(0.5, -0.5, -0.5),
                        Vector(0, 0, 0),
                    ),
            )

        val blockOffsets =
            listOf(
                Vector(0.49, 0.49, 0.49),
                Vector(0.49, 0.49, -0.49),
                Vector(0.49, -0.49, 0.49),
                Vector(0.49, -0.49, -0.49),
                Vector(-0.49, 0.49, 0.49),
                Vector(-0.49, 0.49, -0.49),
                Vector(-0.49, -0.49, 0.49),
                Vector(-0.49, -0.49, -0.49),
            )

        val Block.typeFast: Material
            get() {
                val craftWorld = (world as CraftWorld).handle
                val serverChunkCache = (craftWorld as ServerLevel).getChunkSource()
                val chunk =
                    serverChunkCache.getChunkAtIfLoadedMainThread(this.x shr 4, this.z shr 4) ?: return Material.AIR
                val blockState = chunk.getBlockState(BlockPos(this.x, this.y, this.z))
                return blockState.bukkitMaterial
            }

        fun canSee(eye: Location, loc: Location): Set<BlockPosition>? {
            // Get the center
            val blockCenterLoc = loc.clone().add(0.5, 0.5, 0.5)

            val eyeOffsets =
                if (blockCenterLoc.toVector().subtract(eye.toVector()).dot(eye.direction) < 0) {
                    // Simplify logic if the block is behind the player
                    // There's no reason to add a small margin if the block is unlikely to be seen soon anyway
                    setOf(Vector(0, 0, 0))
                } else {
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
                    eyeOffsetMap.values.elementAt(bestIndex)
                }

            val blockingBlocks: MutableSet<BlockPosition> = mutableSetOf()
            for (eyeOffset in eyeOffsets) {
                val offsetEye = eye.clone().add(eyeOffset)
                for (offset in blockOffsets) {
                    val blockLoc = blockCenterLoc.clone().add(offset)
                    val direction = blockLoc.toVector().subtract(offsetEye.toVector()).normalize()
                    val eyeFacing = offsetEye.clone().setDirection(direction)
                    val maxDistance = min(ceil(eyeFacing.distance(blockLoc)).toInt(), 140)
                    val blockIterator =
                        try {
                            BlockIterator(eyeFacing, 0.0, maxDistance)
                        } catch (_: IllegalStateException) {
                            return null
                        }

                    var visible = true
                    while (blockIterator.hasNext()) {
                        val hitBlock = blockIterator.next()
                        if (hitBlock == loc.block) {
                            break
                        }
                        val hitBlockType = hitBlock.typeFast
                        if (hitBlockType.isOccluding) {
                            if (eyeOffset == Vector(0, 0, 0)) {
                                blockingBlocks += hitBlock.location.toBlockPosition()
                            }
                            visible = false
                            break
                        }
                    }

                    if (visible) return null
                }
            }
            return blockingBlocks
        }

        data class BlockPosition(var x: Int, var y: Int, var z: Int) {
            fun toLocation(world: World) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        }

        fun Location.toBlockPosition() = BlockPosition(blockX, blockY, blockZ)
    }

    val hiddenBlocksByPlayer: MutableMap<UUID, MutableSet<BlockPosition>> = ConcurrentHashMap()
    val playersByHiddenBlock: MutableMap<BlockPosition, MutableSet<UUID>> = ConcurrentHashMap()

    /**
     * Outer map key: A hidden block
     *
     * Inner map key: A block that is preventing the hidden block to be seen
     */
    val blockingBlockForPlayersByHiddenBlock: MutableMap<BlockPosition, MutableMap<BlockPosition, MutableSet<UUID>>> =
        ConcurrentHashMap()

    fun addPos(uuid: UUID, pos: BlockPosition, blockingPositions: Set<BlockPosition>) {
        hiddenBlocksByPlayer.computeIfAbsent(uuid) { ConcurrentHashMap.newKeySet() }.add(pos)
        playersByHiddenBlock.computeIfAbsent(pos) { ConcurrentHashMap.newKeySet() }.add(uuid)
        val entry = blockingBlockForPlayersByHiddenBlock.computeIfAbsent(pos) { ConcurrentHashMap() }
        blockingPositions.forEach { entry.computeIfAbsent(it) { ConcurrentHashMap.newKeySet() }.add(uuid) }
    }

    fun removePos(uuid: UUID, pos: BlockPosition, skipBlockBlockedByForPlayer: Boolean = false) {
        hiddenBlocksByPlayer[uuid]?.remove(pos)
        playersByHiddenBlock[pos]?.remove(uuid)
        if (!skipBlockBlockedByForPlayer) {
            blockingBlockForPlayersByHiddenBlock[pos]?.entries?.removeAll { (_, uuids) ->
                uuids.remove(uuid)
                uuids.isEmpty()
            }
        }

        if (hiddenBlocksByPlayer[uuid]?.isEmpty() == true) hiddenBlocksByPlayer.remove(uuid)
        if (playersByHiddenBlock[pos]?.isEmpty() == true) playersByHiddenBlock.remove(pos)
        if (!skipBlockBlockedByForPlayer) {
            if (blockingBlockForPlayersByHiddenBlock[pos]?.isEmpty() == true)
                blockingBlockForPlayersByHiddenBlock.remove(pos)
        }
    }

    fun removeAllPos(uuid: UUID) {
        val hiddenBlocks = hiddenBlocksByPlayer[uuid] ?: return
        hiddenBlocks.forEach {
            playersByHiddenBlock.computeIfPresent(it) { _, uuids ->
                uuids.remove(uuid)
                if (uuids.isEmpty()) null else uuids
            }
        }
        hiddenBlocksByPlayer.remove(uuid)
        blockingBlockForPlayersByHiddenBlock.entries.removeAll {
            it.value.values.removeAll { uuids ->
                uuids.remove(uuid)
                uuids.isEmpty()
            }
            it.value.isEmpty()
        }
    }

    fun removeOutOfRangeBlocks(player: Player) {
        val threshold = min(player.viewDistance, Bukkit.getViewDistance()) + 1
        val chunkX = player.location.x.toInt() shr 4
        val chunkZ = player.location.z.toInt() shr 4
        playersByHiddenBlock
            .filterKeys { abs(chunkX - (it.x shr 4)) > threshold || abs(chunkZ - (it.z shr 4)) > threshold }
            .forEach { (pos, uuids) -> uuids.forEach { uuid -> removePos(uuid, pos) } }
    }

    fun removeIfNeeded(pos: BlockPosition, world: World) {
        if (world.getBlockAt(pos.toLocation(world)).type !in AntiPieRay.hideMaterials) {
            playersByHiddenBlock
                .filterKeys { pos == it }
                .values
                .forEach { uuids -> uuids.forEach { uuid -> removePos(uuid, pos) } }
        }
    }

    fun updateBlockVisibility(player: Player) {
        val blockList = hiddenBlocksByPlayer[player.uniqueId] ?: return
        blockList.forEach {
            val loc = it.toLocation(player.world)
            val blockingBlock = canSee(player.eyeLocation, loc)
            if (blockingBlock != null) {
                return@forEach
            }
            val block = player.world.getBlockAt(loc)
            removePos(player.uniqueId, it)
            player.sendBlockChange(loc, block.blockData)
        }
    }

    fun updateBlockVisibility(pos: BlockPosition, world: World) {
        removeIfNeeded(pos, world)
        blockingBlockForPlayersByHiddenBlock.entries.removeAll { (blockPos, entry) ->
            val blockLoc = blockPos.toLocation(world)
            val posEntry = entry[pos]
            posEntry?.removeAll { uuid ->
                val player = Bukkit.getPlayer(uuid) ?: return@removeAll false
                val blockingBlockPositions = canSee(player.eyeLocation, blockLoc)
                if (blockingBlockPositions != null) {
                    blockingBlockPositions.forEach { blockingBlockPos ->
                        entry.computeIfAbsent(blockingBlockPos) { ConcurrentHashMap.newKeySet() }.add(uuid)
                    }
                    return@removeAll true
                }
                val block = world.getBlockAt(blockLoc)
                removePos(uuid, blockPos, true)
                player.sendBlockChange(blockLoc, block.blockData)
                true
            }
            posEntry?.isEmpty() == true
        }
    }
}
