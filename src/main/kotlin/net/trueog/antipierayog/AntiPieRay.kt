package net.trueog.antipierayog

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.protocol.world.states.type.StateType
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import java.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class AntiPieRay : JavaPlugin() {
    companion object {
        lateinit var plugin: AntiPieRay
        lateinit var blockEntityHider: BlockEntityHider
        val hideMaterials: MutableSet<Material> =
            Collections.newSetFromMap<Material>(IdentityHashMap()).apply {
                addAll(
                    listOf(
                        // <editor-fold desc="Block entities">
                        Material.CHEST,
                        Material.TRAPPED_CHEST,
                        Material.SHULKER_BOX,
                        Material.WHITE_SHULKER_BOX,
                        Material.ORANGE_SHULKER_BOX,
                        Material.MAGENTA_SHULKER_BOX,
                        Material.LIGHT_BLUE_SHULKER_BOX,
                        Material.YELLOW_SHULKER_BOX,
                        Material.LIME_SHULKER_BOX,
                        Material.PINK_SHULKER_BOX,
                        Material.GRAY_SHULKER_BOX,
                        Material.LIGHT_GRAY_SHULKER_BOX,
                        Material.CYAN_SHULKER_BOX,
                        Material.PURPLE_SHULKER_BOX,
                        Material.BLUE_SHULKER_BOX,
                        Material.BROWN_SHULKER_BOX,
                        Material.GREEN_SHULKER_BOX,
                        Material.RED_SHULKER_BOX,
                        Material.BLACK_SHULKER_BOX,
                        Material.CAMPFIRE,
                        Material.SOUL_CAMPFIRE,
                        Material.BEACON,
                        Material.ENCHANTING_TABLE,
                        Material.DRAGON_HEAD,
                        Material.DRAGON_WALL_HEAD,
                        Material.CONDUIT,
                        Material.BELL,
                        Material.ENDER_CHEST,
                        Material.SPAWNER,
                        // </editor-fold>
                    )
                )
            }
        val hideStateTypes: MutableSet<StateType> = Collections.newSetFromMap(IdentityHashMap(hideMaterials.size))

        private var chunkDataHandle: PacketListenerCommon? = null
        private var blockChangeHandle: PacketListenerCommon? = null
        private var playerPosRotHandle: PacketListenerCommon? = null
    }

    override fun onEnable() {
        plugin = this
        blockEntityHider = BlockEntityHider()

        hideMaterials.forEach {
            SpigotConversionUtil.fromBukkitItemMaterial(it).placedType?.let { element -> hideStateTypes.add(element) }
        }

        val eventManager = PacketEvents.getAPI().eventManager
        chunkDataHandle = eventManager.registerListener(ChunkDataPacketListener(), PacketListenerPriority.NORMAL)
        blockChangeHandle = eventManager.registerListener(BlockChangePacketListener(), PacketListenerPriority.NORMAL)
        playerPosRotHandle =
            eventManager.registerListener(PlayerPositionRotationPacketListener(), PacketListenerPriority.MONITOR)

        server.pluginManager.registerEvents(Events(), this)
    }

    override fun onDisable() {
        Bukkit.getScheduler().cancelTasks(this)

        val eventManager = PacketEvents.getAPI().eventManager
        chunkDataHandle?.let { eventManager.unregisterListener(it) }
        blockChangeHandle?.let { eventManager.unregisterListener(it) }
        playerPosRotHandle?.let { eventManager.unregisterListener(it) }
        chunkDataHandle = null
        blockChangeHandle = null
        playerPosRotHandle = null
    }
}
