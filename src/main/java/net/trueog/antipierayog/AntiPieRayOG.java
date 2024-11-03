package net.trueog.antipierayog;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import net.trueog.antipierayog.command.AntiPieRayCommand;
import net.trueog.antipierayog.handler.Injector;
import net.trueog.antipierayog.listeners.PlayerListener;

/**
 * AntiPieRay main class.
 */
public final class AntiPieRayOG extends JavaPlugin {

	// the config
	protected final AntiPieRayConfig config;

	// the injection manager
	public final Injector injector = new Injector(this);

	private PlayerListener playerListener;

	{
		// load configuration
		config = new AntiPieRayConfig(this);
	}

	// get the config
	public AntiPieRayConfig config() {
		return config;
	}

	///////////////////////////////

	@Override
	public void onEnable() {
		// reload config
		config.reload();

		// enable commands
		setExecutor("antipieray", new AntiPieRayCommand(this));

		// register listeners
		getServer().getPluginManager().registerEvents(playerListener = new PlayerListener(this), this);
	}

	// Sets the given executor/tab completer for the given command
	private void setExecutor(String command, Object executor) {
		PluginCommand cmd = getCommand(command);
		if (cmd == null) {
			throw new IllegalArgumentException("No command by name `" + command + "`");
		}

		if (executor instanceof CommandExecutor e)
			cmd.setExecutor(e);
		if (executor instanceof TabCompleter t)
			cmd.setTabCompleter(t);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(playerListener);

		// remove listeners
		for (Player player : Bukkit.getOnlinePlayers()) {
			injector.uninject(player);
		}
	}

}