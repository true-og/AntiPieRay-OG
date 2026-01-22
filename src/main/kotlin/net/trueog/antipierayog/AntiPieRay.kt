package net.trueog.antipierayog

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.protocol.world.states.type.StateType
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import java.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class AntiPieRay : JavaPlugin() {
    companion object {
        lateinit var plugin: AntiPieRay
        lateinit var blockEntityHider: BlockEntityHider
        val hideStateTypes: MutableSet<StateType> = Collections.newSetFromMap(IdentityHashMap())
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
    }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
        // Has to be here otherwise the plugin doesn't load
        hideStateTypes.addAll(
            listOf(
                // <editor-fold desc="Block entities">
                StateTypes.CHEST,
                StateTypes.TRAPPED_CHEST,
                StateTypes.SHULKER_BOX,
                StateTypes.WHITE_SHULKER_BOX,
                StateTypes.ORANGE_SHULKER_BOX,
                StateTypes.MAGENTA_SHULKER_BOX,
                StateTypes.LIGHT_BLUE_SHULKER_BOX,
                StateTypes.YELLOW_SHULKER_BOX,
                StateTypes.LIME_SHULKER_BOX,
                StateTypes.PINK_SHULKER_BOX,
                StateTypes.GRAY_SHULKER_BOX,
                StateTypes.LIGHT_GRAY_SHULKER_BOX,
                StateTypes.CYAN_SHULKER_BOX,
                StateTypes.PURPLE_SHULKER_BOX,
                StateTypes.BLUE_SHULKER_BOX,
                StateTypes.BROWN_SHULKER_BOX,
                StateTypes.GREEN_SHULKER_BOX,
                StateTypes.RED_SHULKER_BOX,
                StateTypes.BLACK_SHULKER_BOX,
                StateTypes.CAMPFIRE,
                StateTypes.SOUL_CAMPFIRE,
                StateTypes.BEACON,
                StateTypes.ENCHANTING_TABLE,
                StateTypes.DRAGON_HEAD,
                StateTypes.DRAGON_WALL_HEAD,
                StateTypes.CONDUIT,
                StateTypes.BELL,
                StateTypes.ENDER_CHEST,
                StateTypes.SPAWNER,
                // </editor-fold>
            )
        )
        PacketEvents.getAPI().eventManager.registerListener(ChunkDataPacketListener(), PacketListenerPriority.NORMAL)
        PacketEvents.getAPI().eventManager.registerListener(BlockChangePacketListener(), PacketListenerPriority.NORMAL)
        PacketEvents.getAPI()
            .eventManager
            .registerListener(PlayerPositionRotationPacketListener(), PacketListenerPriority.MONITOR)
    }

    override fun onEnable() {
        PacketEvents.getAPI().init()

        plugin = this
        blockEntityHider = BlockEntityHider()

        server.pluginManager.registerEvents(Events(), this)
    }

    override fun onDisable() {
        Bukkit.getScheduler().cancelTasks(this)
        PacketEvents.getAPI().terminate()
    }
}
