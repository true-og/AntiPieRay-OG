package net.trueog.antiPieRayOG

import net.trueog.antiPieRayOG.BlockEntityHider.Companion.toBlockPosition
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent

class Events : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val worldName = event.player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        AntiPieRay.blockEntityHider.updateBlockVisibility(event.player)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val worldName = event.player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        Bukkit.getScheduler()
            .runTask(
                AntiPieRay.plugin,
                Runnable {
                    AntiPieRay.blockEntityHider.updateBlockVisibility(
                        event.block.location.toBlockPosition(),
                        event.block.world,
                    )
                },
            )
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        val worldName = event.player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        AntiPieRay.blockEntityHider.removeAllPos(event.player.uniqueId)
    }
}
