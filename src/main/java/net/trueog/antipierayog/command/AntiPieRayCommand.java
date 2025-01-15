package net.trueog.antipierayog.command;

import static net.trueog.antipierayog.AntiPieRayOG.PREFIX;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.trueog.antipierayog.AntiPieRayOG;
import net.trueog.antipierayog.handler.PlayerBlockEntityHandler;
import net.trueog.utilitiesog.UtilitiesOG;

public class AntiPieRayCommand extends BaseCommand {

    // Container for the plugin instance.
    final AntiPieRayOG plugin;

    public AntiPieRayCommand(AntiPieRayOG plugin) {

        super(plugin, "antipieray", "apr");
        this.plugin = plugin;

        executes(ctx -> {

            ctx.sender().sendMessage(UtilitiesOG.trueogColorize(PREFIX + "&fAntiPieRay by @orbyfied on GitHub"));

        });

        then(subcommand("reload").permission("antipieray.admin").executes(ctx -> {

            var sender = ctx.sender();

            // Reload configuration.
            if (plugin.config().reload()) {

                sender.sendMessage(UtilitiesOG.trueogColorize(PREFIX + "&aSuccessfully reloaded configuration"));

            } else {

                sender.sendMessage(UtilitiesOG.trueogColorize(PREFIX + "&cFailed to reload configuration"));

                sender.sendMessage(UtilitiesOG
                        .trueogColorize(PREFIX + "&c● A full stacktrace describing the error should be in console."));

            }

        }));

        then(subcommand("debug").permission("antipieray.admin")
                .then(subcommand("lshidden").with(Argument.onlinePlayer("player")).executes(ctx -> {

                    Player player = ctx.get("player", Player.class).get();
                    var handler = plugin.injector.getHandler(player);
                    if (handler == null) {

                        ctx.fail("No injected handler for given player");

                        return;

                    }

                    CommandSender sender = ctx.sender;
                    for (PlayerBlockEntityHandler.ChunkData chunkData : handler.getChunkDataMap().values()) {

                        // Send chunk header.
                        int cx = (int) (chunkData.pos & 0xFFFFFFFF00000000L);
                        int cz = (int) (chunkData.pos << 32 & 0xFFFFFFFF00000000L);

                        sender.sendMessage(
                                "Chunk(" + cx + ", " + cz + ") hidden# = " + chunkData.hiddenEntities.size());

                        // Send all hidden entities.
                        for (int packed : chunkData.hiddenEntities) {

                            int x = packed & 0xFF;
                            packed <<= 8;
                            int y = packed & 0xFFFF;
                            packed <<= 16;
                            int z = packed & 0xFF;

                            sender.sendMessage("  hidden(x: " + x + " y: " + y + " z: " + z + ") blockType = "
                                    + player.getWorld().getBlockAt(x, y, z).getType());

                        }

                    }

                })));

    }

}