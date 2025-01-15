package net.trueog.antipierayog.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Represents a subcommand.
public class Subcommand {

    public static Subcommand subcommand(String name, String... aliases) {

        return new Subcommand(name, aliases);

    }

    // The primary name of this subcommand.
    protected final String name;

    // The aliases for this subcommand.
    protected final String[] aliases;

    // The subcommands under this subcommand.
    protected final Map<String, Subcommand> subcommands = new HashMap<>();

    // The arguments for this subcommand.
    protected final List<Argument<?>> arguments = new ArrayList<>();

    // The flags on this subcommand.
    protected final Map<String, Argument<?>> flags = new HashMap<>();

    // The permission required to run this command.
    protected String permission;

    // The executor for this subcommand.
    protected Executor executor;

    public Subcommand(String name, String[] aliases) {

        this.name = name;
        this.aliases = aliases;

    }

    public Subcommand permission(String permission) {

        this.permission = permission;

        return this;

    }

    public String getPermission() {

        return permission;

    }

    /**
     * Registers the given subcommand and it's aliases as a subcommand to this
     * subcommand.
     *
     * @param subcommand The subcommand.
     * @return This.
     */
    public Subcommand then(Subcommand subcommand) {

        subcommands.put(subcommand.name, subcommand);
        for (String alias : subcommand.aliases) {

            subcommands.put(alias, subcommand);

        }

        // Propagate permission.
        if (subcommand.permission == null) {

            subcommand.permission = this.permission;

        }

        return this;

    }

    /**
     * Registers the given argument or flag to this subcommand.
     *
     * @param argument The argument.
     * @return This.
     */
    public Subcommand with(Argument<?> argument) {

        if (argument.isFlag()) {

            flags.put(argument.name(), argument);

            for (String alias : argument.aliases()) {

                flags.put(alias, argument);

            }

        } else {

            arguments.add(argument);

        }

        return this;

    }

    public Map<String, Subcommand> getSubcommands() {
        return subcommands;

    }

    public String[] getAliases() {

        return aliases;

    }

    public List<Argument<?>> getArguments() {

        return arguments;

    }

    public String getName() {

        return name;

    }

    public Subcommand executes(Executor executor) {

        this.executor = executor;

        return this;

    }

}