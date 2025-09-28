package net.trueog.antiPieRayOG

import net.trueog.antiPieRayOG.BlockEntityHider.Companion.toBlockPosition
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent

class Events : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val worldName = event.player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        Bukkit.getScheduler()
            .runTaskLaterAsynchronously(
                AntiPieRay.plugin,
                Runnable { AntiPieRay.blockEntityHider.updateBlockVisibility(event.player) },
                1L,
            )
    }

    fun genericBlockBreakHandler(block: Block) {
        val worldName = block.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        Bukkit.getScheduler()
            .runTaskLaterAsynchronously(
                AntiPieRay.plugin,
                Runnable {
                    AntiPieRay.blockEntityHider.updateBlockVisibility(block.location.toBlockPosition(), block.world)
                },
                1L,
            )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBreak(event: BlockBreakEvent) {
        genericBlockBreakHandler(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBurn(event: BlockBurnEvent) {
        genericBlockBreakHandler(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockExplode(event: BlockExplodeEvent) {
        genericBlockBreakHandler(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        val worldName = event.player.world.name
        if (worldName != "world" && worldName != "world_nether" && worldName != "world_the_end") return

        AntiPieRay.blockEntityHider.removeAllPos(event.player.uniqueId)
    }
}
