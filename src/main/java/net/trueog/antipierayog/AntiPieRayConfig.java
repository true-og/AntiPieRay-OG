package net.trueog.antipierayog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.trueog.antipierayog.config.Configuration;

public class AntiPieRayConfig {

    AntiPieRayConfig(AntiPieRayOG plugin) {
        this.plugin = plugin;

        // Create config.
        config = new Configuration(plugin.getDataFolder().toPath().resolve("config.yml"), plugin::getResource)
                .reloadOrDefaultThrowing("defaults/config.yml");

    }

    // Declare plugin instance.
    final AntiPieRayOG plugin;

    // Declare config file instance.
    public final Configuration config;

    // Declare fields for fast object access.
    public volatile Set<BlockEntityType<?>> checkedBlockEntities = new HashSet<>();
    public volatile Set<Block> checkedBlockTypes = new HashSet<>();
    public volatile double alwaysViewDist;
    public volatile double alwaysViewDistSqr;

    /**
     * Reload the configuration.
     *
     * @return If it succeeded.
     */
    public boolean reload() {

        synchronized (this) {

            try {

                // Reload from file.
                config.reloadOrDefaultThrowing("defaults/config.yml");

                checkedBlockEntities.clear();
                checkedBlockTypes.clear();

                config.getOrSupply("checked-block-entities", (Supplier<ArrayList<String>>) ArrayList::new)
                        .forEach(s -> {

                            final ResourceLocation loc = ResourceLocation.of(s, ':');

                            checkedBlockEntities.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(loc));
                            checkedBlockTypes.add(BuiltInRegistries.BLOCK.get(loc));

                        });

                alwaysViewDist = config.get("always-view-distance");
                alwaysViewDistSqr = alwaysViewDist * alwaysViewDist;

                // Return success state.
                return true;

            } catch (Exception error) {

                plugin.getLogger().warning("Failed to reload configuration, uncaught error.");

                error.printStackTrace();

                return false;

            }

        }

    }

}