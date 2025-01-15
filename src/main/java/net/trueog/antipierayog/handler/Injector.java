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

        // Remove handler.
        PlayerBlockEntityHandler handler = handlerMap.remove(player.getUniqueId());

        handler.player.connection.connection.channel.pipeline().remove(handler);

    }

    public PlayerBlockEntityHandler getHandler(UUID uuid) {

        return handlerMap.get(uuid);

    }

    public PlayerBlockEntityHandler getHandler(Player player) {

        return handlerMap.get(player.getUniqueId());

    }

}