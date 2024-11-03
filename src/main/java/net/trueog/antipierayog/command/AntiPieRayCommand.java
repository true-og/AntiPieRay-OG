package net.trueog.antipierayog.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.trueog.antipierayog.AntiPieRayOG;
import net.trueog.antipierayog.handler.PlayerBlockEntityHandler;
import net.trueog.utilitiesog.UtilitiesOG;

public class AntiPieRayCommand extends BaseCommand {

	private static String prefix = "&8[&cAntiPieRay&4-OG&8] ";

	public AntiPieRayCommand(AntiPieRayOG plugin) {
		super(plugin, "antipieray", "apr");
		this.plugin = plugin;

		// Executing the main command
		executes(ctx -> ctx.sender().sendMessage(UtilitiesOG.trueogExpand(prefix + "&cAntiPieRay&4-OG &eby TrueOG Network forked from AntiPieRay by &6@orbyfied &eon GitHub.")));

		// Reload subcommand
		then(subcommand("reload")
				.permission("antipieray.admin")
				.executes(ctx -> {
					CommandSender sender = ctx.sender();

					// reload configuration
					if (plugin.config().reload()) {

						if(sender instanceof Player) {

							UtilitiesOG.trueogMessage((Player) sender, prefix + "<green>Successfully reloaded configuration.");

						}
						else {

							UtilitiesOG.logToConsole(prefix, "Successfully reloaded configuration.");

						}

					}
					else {

						if(sender instanceof Player) {

							UtilitiesOG.trueogMessage((Player) sender, prefix + "<red>Failed to reload configuration!");
							UtilitiesOG.trueogMessage((Player) sender, prefix + "<red>● A full stacktrace describing the error should be in console.");

						}
						else {

							UtilitiesOG.logToConsole(prefix, "Failed to reload configuration!");

						}

					}

				}));

		// Debug subcommand.
		then(subcommand("debug")
				.permission("antipieray.admin")
				.then(subcommand("lshidden")
						.with(Argument.onlinePlayer("player"))
						.executes(ctx -> {
							Player player = ctx.get("player", Player.class).get();
							var handler = plugin.injector.getHandler(player);

							if (handler == null) {
								ctx.fail("No injected handler for given player");
								return;
							}

							for (PlayerBlockEntityHandler.ChunkData chunkData : handler.getChunkDataMap().values()) {
								// Send chunk header
								int cx = (int)(chunkData.pos & 0xFFFFFFFF00000000L);
								int cz = (int)(chunkData.pos << 32 & 0xFFFFFFFF00000000L);
								UtilitiesOG.trueogMessage(player, ("Chunk(" + cx + ", " + cz + ") hidden# = " + chunkData.hiddenEntities.size()));

								// Send all hidden entities
								for (int packed : chunkData.hiddenEntities) {
									int x = packed & 0xFF;   packed <<= 8;
									int y = packed & 0xFFFF; packed <<= 16;
									int z = packed & 0xFF;
									UtilitiesOG.trueogMessage(player, ("  hidden(x: " + x + " y: " + y + " z: " + z + ") blockType = " 
											+ player.getWorld().getBlockAt(x, y, z).getType()));
								}
							}

						})));

	}

	// the plugin instance
	final AntiPieRayOG plugin;

}