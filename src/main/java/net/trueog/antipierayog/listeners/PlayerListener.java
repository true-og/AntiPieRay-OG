package net.trueog.antipierayog.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.trueog.antipierayog.AntiPieRayOG;

public class PlayerListener implements Listener {

    public PlayerListener(AntiPieRayOG plugin) {

        this.plugin = plugin;

    }

    // The plugin instance.
    final AntiPieRayOG plugin;

    @EventHandler
    void onJoin(PlayerJoinEvent event) {

        plugin.injector.inject(event.getPlayer());

    }

    @EventHandler
    void onLeave(PlayerQuitEvent event) {

        plugin.injector.uninject(event.getPlayer());

    }

}