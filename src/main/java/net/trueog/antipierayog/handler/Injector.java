package net.trueog.antipierayog.handler;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.entity.Player;

import net.minecraft.server.level.ServerPlayer;
import net.trueog.antipierayog.AntiPieRayOG;
import net.trueog.antipierayog.util.NmsHelper;

public class Injector {

    public static final String HANDLER_ID = "AntiPieRay_packet_handler";

    // The plugin instance.
    final AntiPieRayOG plugin;

    public Injector(AntiPieRayOG plugin) {

        this.plugin = plugin;

    }

    // The packet handlers by player.
    final Map<UUID, PlayerBlockEntityHandler> handlerMap = new WeakHashMap<>();

    /**
     * Instantiates, injects and enables the handler for the given player.
     *
     * @param player The player.
     */
    public void inject(Player player) {

        // Get NMS player.
        ServerPlayer nmsPlayer = NmsHelper.getPlayerHandle(player);

        // Create handler.
        PlayerBlockEntityHandler packetHandler = new PlayerBlockEntityHandler(this, nmsPlayer);

        // Populate hander.
        handlerMap.put(player.getUniqueId(), packetHandler);

        // Inject handler.
        nmsPlayer.connection.connection.channel.pipeline().addLast(HANDLER_ID, packetHandler);

    }

    /**
     * Removes and disables the handler for the given player.
     *
     * @param player The player.
     */
    public void uninject(Player player) {

        // Remove handler from the map.
        PlayerBlockEntityHandler handler = handlerMap.remove(player.getUniqueId());

        // If the handler is null, there's nothing left to uninject.
        if (handler == null) {

            return;

        }

        // Check if the pipeline still has that handler ID.
        if (handler.player != null && handler.player.connection != null && handler.player.connection.connection != null
                && handler.player.connection.connection.channel != null
                && handler.player.connection.connection.channel.pipeline().get(HANDLER_ID) != null) {

            // Pipe cleaner.
            handler.player.connection.connection.channel.pipeline().remove(HANDLER_ID);

        }

    }

    public PlayerBlockEntityHandler getHandler(UUID uuid) {

        return handlerMap.get(uuid);

    }

    public PlayerBlockEntityHandler getHandler(Player player) {

        return handlerMap.get(player.getUniqueId());

    }

}