package net.trueog.antipierayog.config;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.yaml.snakeyaml.Yaml;

import net.trueog.antipierayog.util.Throwables;

/**
 * File-based configuration wrapped for ease of use.
 *
 * @author orbyfied
 */
@SuppressWarnings({ "unchecked" })
public class Configuration implements Section {

    static final Yaml YAML = new Yaml();

    /**
     * The resource resolver.
     */
    final Function<String, InputStream> resourceResolver;

    /**
     * The file that is supposed to hold / holds this configuration on the disk.
     */
    final Path file;

    /**
     * The internal configuration.
     */
    Map<String, Object> map;

    /**
     * Constructor.
     *
     * @param f The file of the configuration.
     */
    public Configuration(Path f, Function<String, InputStream> resourceResolver) {

        this.file = f;
        this.resourceResolver = resourceResolver;

        // Create empty map.
        this.map = new HashMap<>();

    }

    // Basic Getters.
    public Path getFile() {

        return file;

    }

    public Map<String, Object> map() {

        return map;

    }

    /**
     * (Re)loads the configuration from the file. If the file does not exist, it
     * saves the defaults.
     *
     * @return This.
     */
    public Configuration reloadOrDefault(String defaults) {

        // Check if file exists.
        if (!Files.exists(file)) {

            trySaveDefault(defaults, false);

        }

        // Load configuration.
        tryReload();

        return this;

    }

    /**
     * (Re)loads the configuration from the file. If the file does not exist, it
     * saves the defaults.
     *
     * @return This.
     */
    public Configuration reloadOrDefaultThrowing(String defaults) {

        // Check if file exists.
        if (!Files.exists(file)) {

            trySaveDefault(defaults, false);

        }

        // Load configuration.
        reload();

        return this;

    }

    /**
     * (Re)loads the configuration from the file.
     *
     * @return This.
     */
    public Configuration reload() {

        // Check if file exists.
        if (!Files.exists(file)) {

            throw new IllegalArgumentException("Supposed configuration file " + file + " doesnt exist");

        }

        try {

            // Load configuration.
            FileReader reader = new FileReader(this.file.toFile());

            this.map = YAML.load(reader);

            reader.close();

        } catch (Exception error) {

            Throwables.sneakyThrow(error);

        }

        return this;

    }

    /**
     * (Re)loads the configuration from the file or creates a new empty file if it
     * doesn't exist.
     *
     * @return This.
     */
    public Configuration reloadOrCreate() {

        // Check if file exists.
        if (!Files.exists(file)) {

            try {

                if (!Files.exists(file.getParent())) {

                    Files.createDirectories(file.getParent());

                }

                Files.createFile(file);

            } catch (IOException e) {

                Throwables.sneakyThrow(e);

            }

        }

        try {

            // Load configuration.
            FileReader reader = new FileReader(this.file.toFile());

            this.map = YAML.load(reader);

            reader.close();

        } catch (Exception error) {

            Throwables.sneakyThrow(error);

        }

        return this;

    }

    /**
     * (Re)loads the configuration from the file.
     *
     * @return This.
     */
    public Configuration tryReload() {

        // Check if file exists.
        if (!Files.exists(file)) {

            return this;

        }

        try {

            // Load configuration.
            FileReader reader = new FileReader(this.file.toFile());

            this.map = YAML.load(reader);

            reader.close();

        } catch (Exception error) {

            return this;

        }

        return this;

    }

    /**
     * Loads the configuration from the given input stream.
     *
     * @param inputStream The input stream.
     * @return This.
     */
    public Configuration loadFrom(InputStream inputStream) {

        Objects.requireNonNull(inputStream, "Input stream cannot be null");

        try {

            // Load configuration.
            InputStreamReader reader = new InputStreamReader(inputStream);

            this.map = YAML.load(reader);

            reader.close();

        } catch (Exception error) {

            Throwables.sneakyThrow(error);

        }

        return this;

    }

    /**
     * Saves the configuration to the file.
     *
     * @return This.
     * @throws IllegalArgumentException if the file is invalid, making it unable to
     *                                  be created
     * @throws IllegalStateException    if the saving fails with an IOException
     */
    public Configuration save() {

        // Check if the file doesn't exist.
        Path file = this.file.toAbsolutePath();
        if (!Files.exists(file)) {

            try {

                // Attempt to create the file.
                if (!Files.exists(file.getParent())) {

                    Files.createDirectories(file.getParent());

                }

                Files.createFile(file);

            } catch (Exception error) {

                throw new IllegalArgumentException("failed to create non-existent file " + file, error);

            }

        }

        // Try to save the file.
        try {

            FileWriter writer = new FileWriter(file.toFile());

            YAML.dump(map, writer);

            writer.close();

        } catch (IOException error) {

            throw new IllegalStateException("failed to save configuration to file " + file, error);

        }

        return this;

    }

    /**
     * Tries to save the default configuration, given by a resource which should be
     * in your JAR file, named after the file.
     *
     * @param defaults The resource path for the defaults.
     * @param override If an existent file should be overwritten.
     * @return This.
     */
    public Configuration trySaveDefault(String defaults, boolean override) {

        // Check override.
        if (!override && Files.exists(file)) {

            return this;

        }

        try {

            // Open resource.
            InputStream is = resourceResolver.apply(defaults);
            if (is == null) {

                throw new IllegalArgumentException("Could not open resource '" + defaults + "'");

            }

            // Create file if non-existent.
            Path file = this.file.toAbsolutePath();
            if (!Files.exists(file)) {

                if (!Files.exists(file.getParent())) {

                    Files.createDirectories(file.getParent());

                }

                Files.createFile(file);

            }

            // Open file, write and close.
            OutputStream out = Files.newOutputStream(file);

            is.transferTo(out);
            is.close();

            out.close();

        } catch (Exception error) {

            throw new IllegalStateException("failed to save configuration to file " + file, error);

        }

        return this;

    }

    //////////////////////////////////////
    ////////// WRAPPED METHODS ///////////
    //////////////////////////////////////
    @Override
    public Set<String> getKeys() {

        return map.keySet();

    }

    @Override
    public Collection<Object> getValues() {

        return map.values();

    }

    @Override
    public <T> T get(String key) {

        return (T) map.get(key);

    }

    @Override
    public <T> T getOrDefault(String key, T def) {

        return (T) map.getOrDefault(key, def);

    }

    @Override
    public <T> T getOrSupply(String key, Supplier<T> def) {

        if (!map.containsKey(key)) {

            return def.get();

        }

        return (T) map.get(key);

    }

    @Override
    public void set(String key, Object value) {

        map.put(key, value);

    }

    @Override
    public boolean contains(String key) {

        return map.containsKey(key);

    }

    @Override
    public Section section(String key) {

        Object v = map.get(key);
        if (v == null) {

            Map<String, Object> map = new HashMap<>();

            this.map.put(key, map);

            return Section.memory(map);

        } else if (v instanceof Map map) {

            return Section.memory(map);

        } else if (v instanceof Section section) {

            return section;

        }

        throw new IllegalStateException("Value by key '" + key + "' is not a section or map");

    }

}