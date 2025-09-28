package net.trueog.antiPieRayOG

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.world.chunk.Column
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import net.trueog.antiPieRayOG.BlockEntityHider.Companion.BlockPosition
import org.bukkit.Location
import org.bukkit.entity.Player

class ChunkDataPacketListener : PacketListener {
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.CHUNK_DATA) {
            return
        }

        val player = event.getPlayer<Player>()

        val worldName = player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        val chunkData = WrapperPlayServerChunkData(event)
        val column = chunkData.column
        val chunks = column.chunks

        val tileEntitiesToRemove = mutableSetOf<BlockPosition>()
        chunks.forEachIndexed { sectionIndex, baseChunk ->
            val sectionBaseY = sectionIndex * 16 - 64
            for (x in 0 until 16) {
                for (y in 0 until 16) {
                    for (z in 0 until 16) {
                        val blockState = baseChunk.get(x, y, z)
                        if (blockState.type !in AntiPieRay.hideStateTypes) {
                            continue
                        }

                        val absoluteX = column.x * 16 + x
                        val absoluteY = sectionBaseY + y
                        val absoluteZ = column.z * 16 + z

                        val blockingBlockPositions =
                            BlockEntityHider.canSee(
                                player.eyeLocation,
                                Location(player.world, absoluteX.toDouble(), absoluteY.toDouble(), absoluteZ.toDouble()),
                            )
                        if (blockingBlockPositions != null) {
                            val blockPos = BlockPosition(absoluteX, absoluteY, absoluteZ)
                            tileEntitiesToRemove += blockPos
                            AntiPieRay.blockEntityHider.addPos(player.uniqueId, blockPos, blockingBlockPositions)
                            baseChunk.set(x, y, z, StateTypes.AIR.createBlockState())
                        }
                    }
                }
            }
        }

        if (tileEntitiesToRemove.isEmpty()) {
            return
        }

        val tileEntities = column.tileEntities.toMutableSet()
        tileEntities.removeIf { BlockPosition(it.x, it.y, it.z) in tileEntitiesToRemove }
        val tileEntitiesArray = tileEntities.toTypedArray()

        AntiPieRay.blockEntityHider.removeOutOfRangeBlocks(player)

        @Suppress("DEPRECATION")
        val newColumn =
            when {
                !column.hasHeightMaps() && !column.hasBiomeData() ->
                    Column(column.x, column.z, column.isFullChunk, chunks, tileEntitiesArray)

                column.hasHeightMaps() && !column.hasBiomeData() ->
                    Column(column.x, column.z, column.isFullChunk, chunks, tileEntitiesArray, column.heightMaps)

                !column.hasHeightMaps() && column.hasBiomeData() ->
                    Column(column.x, column.z, column.isFullChunk, chunks, tileEntitiesArray, column.biomeDataBytes)

                else ->
                    Column(
                        column.x,
                        column.z,
                        column.isFullChunk,
                        chunks,
                        tileEntitiesArray,
                        column.heightMaps,
                        column.biomeDataBytes,
                    )
            }

        chunkData.column = newColumn
        event.markForReEncode(true)
    }
}
