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
 * File-based configuration
 * wrapped for ease of use.
 * @author orbyfied
 */
@SuppressWarnings({ "unchecked" })
public class Configuration implements Section {

	static final Yaml YAML = new Yaml();

	///////////////////////////////////////////////////////

	/**
	 * The resource resolver.
	 */
	final Function<String, InputStream> resourceResolver;

	/**
	 * The file that is supposed to hold / holds this
	 * configuration on the disk.
	 */
	final Path file;

	/**
	 * The internal configuration.
	 */
	Map<String, Object> map;

	/**
	 * Constructor.
	 * @param f The file of the configuration.
	 */
	public Configuration(
			Path f,
			Function<String, InputStream> resourceResolver
			) {
		this.file = f;
		this.resourceResolver = resourceResolver;

		// create empty map
		this.map = new HashMap<>();
	}

	/* Basic Getters. */

	public Path getFile()            { return file; }
	public Map<String, Object> map() { return map; }

	/**
	 * (Re)loads the configuration from
	 * the file. If the file does not exist
	 * it saves the defaults.
	 * @return This.
	 */
	public Configuration reloadOrDefault(String defaults) {
		// check if file exists
		if (!Files.exists(file))
			trySaveDefault(defaults, false);
		// load configuration
		tryReload();
		// return
		return this;
	}

	/**
	 * (Re)loads the configuration from
	 * the file. If the file does not exist
	 * it saves the defaults.
	 * @return This.
	 */
	public Configuration reloadOrDefaultThrowing(String defaults) {
		// check if file exists
		if (!Files.exists(file))
			trySaveDefault(defaults, false);
		// load configuration
		reload();
		// return
		return this;
	}

	/**
	 * (Re)loads the configuration from
	 * the file.
	 * @return This.
	 */
	public Configuration reload() {
		// check if file exists
		if (!Files.exists(file))
			throw new IllegalArgumentException("Supposed configuration file " + file + " doesnt exist");

		try {
			// load configuration
			FileReader reader = new FileReader(this.file.toFile());
			this.map = YAML.load(reader);
			reader.close();
		} catch (Exception e) {
			Throwables.sneakyThrow(e);
		}

		// return
		return this;
	}

	/**
	 * (Re)loads the configuration from
	 * the file or creates a new empty
	 * file if it doesn't exist.
	 * @return This.
	 */
	public Configuration reloadOrCreate() {
		// check if file exists
		if (!Files.exists(file)) {
			try {
				if (!Files.exists(file.getParent()))
					Files.createDirectories(file.getParent());
				Files.createFile(file);
			} catch (IOException e) {
				Throwables.sneakyThrow(e);
			}
		}

		try {
			// load configuration
			FileReader reader = new FileReader(this.file.toFile());
			this.map = YAML.load(reader);
			reader.close();
		} catch (Exception e) {
			Throwables.sneakyThrow(e);
		}

		// return
		return this;
	}

	/**
	 * (Re)loads the configuration from
	 * the file.
	 * @return This.
	 */
	public Configuration tryReload() {
		// check if file exists
		if (!Files.exists(file))
			return this;

		try {
			// load configuration
			FileReader reader = new FileReader(this.file.toFile());
			this.map = YAML.load(reader);
			reader.close();
		} catch (Exception e) {
			return this;
		}

		// return
		return this;
	}

	/**
	 * Loads the configuration from the
	 * given input stream.
	 *
	 * @param inputStream The input stream.
	 * @return This.
	 */
	public Configuration loadFrom(InputStream inputStream) {
		Objects.requireNonNull(inputStream, "Input stream can not be null");

		try {
			// load configuration
			InputStreamReader reader = new InputStreamReader(inputStream);
			this.map = YAML.load(reader);
			reader.close();
		} catch (Exception e) {
			Throwables.sneakyThrow(e);
		}

		return this;
	}

	/**
	 * Saves the configuration to
	 * the file.
	 * @return This.
	 * @throws IllegalArgumentException if the file is invalid, making
	 *                                  it unable to be created
	 * @throws IllegalStateException if the saving fails with an IOException
	 */
	public Configuration save() {
		// check if the file doesnt exist
		Path file = this.file.toAbsolutePath();
		if (!Files.exists(file)) {
			try {
				// attempt to create it
				if (!Files.exists(file.getParent()))
					Files.createDirectories(file.getParent());
				Files.createFile(file);
			} catch (Exception e) {
				throw new IllegalArgumentException("failed to create non-existent file " + file, e);
			}
		}

		// try to save
		try {
			FileWriter writer = new FileWriter(file.toFile());
			YAML.dump(map, writer);
			writer.close();
		} catch (IOException e) {
			throw new IllegalStateException("failed to save configuration to file " + file, e);
		}

		// return
		return this;
	}

	/**
	 * Tries to save the default configuration,
	 * given by a resource which should be in
	 * your JAR file, named after the file.
	 * @param defaults The resource path for the defaults.
	 * @param override If an existent file should be
	 *                 overwritten.
	 * @return This.
	 */
	public Configuration trySaveDefault(String defaults, boolean override) {
		// check override
		if (!override && Files.exists(file))
			return this;

		try {
			// open resource
			InputStream is = resourceResolver.apply(defaults);
			if (is == null)
				throw new IllegalArgumentException("Could not open resource '" + defaults + "'");
			// create file if non-existent
			Path file = this.file.toAbsolutePath();
			if (!Files.exists(file)) {
				if (!Files.exists(file.getParent()))
					Files.createDirectories(file.getParent());
				Files.createFile(file);
			}

			// open file, write and close
			OutputStream out = Files.newOutputStream(file);
			is.transferTo(out);
			is.close();
			out.close();
		} catch (Exception e) {
			throw new IllegalStateException("failed to save configuration to file " + file, e);
		}

		// return
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
		if (!map.containsKey(key))
			return def.get();
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