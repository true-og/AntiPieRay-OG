package net.trueog.antipierayog

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PlayerPositionRotationPacketListener : PacketListener {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return
        }

        val player = event.getPlayer<Player>()

        val worldName = player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        Bukkit.getScheduler()
            .runTaskLaterAsynchronously(
                AntiPieRay.plugin,
                Runnable { AntiPieRay.blockEntityHider.updateBlockVisibility(player) },
                1,
            )
    }
}
