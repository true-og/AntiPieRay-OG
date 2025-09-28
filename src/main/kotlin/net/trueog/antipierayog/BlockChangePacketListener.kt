package net.trueog.antipierayog

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import net.trueog.antipierayog.BlockEntityHider.Companion.BlockPosition
import org.bukkit.Location
import org.bukkit.entity.Player

class BlockChangePacketListener : PacketListener {
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.BLOCK_CHANGE) {
            return
        }

        val player = event.getPlayer<Player>()

        val worldName = player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        val blockChange = WrapperPlayServerBlockChange(event)
        if (blockChange.blockState.type !in AntiPieRay.hideStateTypes) {
            return
        }

        val pos = blockChange.blockPosition
        val blockingBlockPositions =
            BlockEntityHider.canSee(
                player.eyeLocation,
                Location(player.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()),
            )

        if (blockingBlockPositions != null) {
            val blockPos = BlockPosition(pos.x, pos.y, pos.z)
            AntiPieRay.blockEntityHider.addPos(player.uniqueId, blockPos, blockingBlockPositions)
            event.isCancelled = true
        }
    }
}
