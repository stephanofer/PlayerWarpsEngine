package com.hera.playerwarps.command;

import com.hera.playerwarps.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandRegistrar {

    private final Plugin plugin;
    private final CommandMap commandMap;
    private final Map<String, Command> knownCommands;
    private DynamicPluginCommand registeredCommand;
    private List<String> registeredLabels = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    public CommandRegistrar(Plugin plugin) throws CommandRegistrationException {
        this.plugin = plugin;

        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            this.commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            Field knownCommandsField = findField(this.commandMap.getClass(), "knownCommands");
            knownCommandsField.setAccessible(true);
            this.knownCommands = (Map<String, Command>) knownCommandsField.get(this.commandMap);
        } catch (Exception exception) {
            throw new CommandRegistrationException("Unable to access Bukkit CommandMap", exception);
        }
    }

    public synchronized void register(Settings settings, PlayerWarpCommand handler) throws CommandRegistrationException {
        if (this.registeredCommand != null) {
            throw new CommandRegistrationException("Command is already registered");
        }
        RegisteredCommand registered = createAndRegister(settings, handler);
        this.registeredCommand = registered.command();
        this.registeredLabels = registered.labels();
    }

    public synchronized void reload(Settings settings, PlayerWarpCommand handler) throws CommandRegistrationException {
        DynamicPluginCommand previousCommand = this.registeredCommand;
        List<String> previousLabels = new ArrayList<String>(this.registeredLabels);

        validatePrimaryAvailable(settings.primaryCommand(), previousCommand);
        List<String> acceptedAliases = acceptedAliases(settings, previousCommand);

        unregisterCurrent();

        try {
            RegisteredCommand registered = createAndRegister(settings.primaryCommand(), acceptedAliases, handler);
            this.registeredCommand = registered.command();
            this.registeredLabels = registered.labels();
        } catch (CommandRegistrationException exception) {
            restorePrevious(previousCommand, previousLabels);
            throw exception;
        }
    }

    public synchronized void unregister() {
        unregisterCurrent();
        this.registeredCommand = null;
        this.registeredLabels = new ArrayList<String>();
    }

    private RegisteredCommand createAndRegister(Settings settings, PlayerWarpCommand handler) throws CommandRegistrationException {
        validatePrimaryAvailable(settings.primaryCommand(), this.registeredCommand);
        List<String> acceptedAliases = acceptedAliases(settings, this.registeredCommand);
        return createAndRegister(settings.primaryCommand(), acceptedAliases, handler);
    }

    private RegisteredCommand createAndRegister(String primaryCommand, List<String> aliases, PlayerWarpCommand handler) throws CommandRegistrationException {
        DynamicPluginCommand command = new DynamicPluginCommand(primaryCommand, aliases, this.plugin, handler);
        this.commandMap.register(this.plugin.getName().toLowerCase(Locale.ENGLISH), command);

        if (this.knownCommands.get(primaryCommand) != command) {
            removeKnownCommandEntries(command);
            throw new CommandRegistrationException("Primary command '/" + primaryCommand + "' is already registered by another plugin");
        }

        List<String> labels = new ArrayList<String>();
        labels.add(primaryCommand);

        for (String alias : aliases) {
            if (this.knownCommands.get(alias) == command) {
                labels.add(alias);
            } else {
                this.plugin.getLogger().warning("Command alias '/" + alias + "' could not be registered and was skipped.");
            }
        }

        return new RegisteredCommand(command, labels);
    }

    private void validatePrimaryAvailable(String primaryCommand, Command allowedCurrentCommand) throws CommandRegistrationException {
        Command existing = this.knownCommands.get(primaryCommand);
        if (existing != null && existing != allowedCurrentCommand) {
            throw new CommandRegistrationException("Primary command '/" + primaryCommand + "' is already registered by another plugin");
        }
    }

    private List<String> acceptedAliases(Settings settings, Command allowedCurrentCommand) {
        return acceptedAliases(settings.primaryCommand(), settings.commandAliases(), allowedCurrentCommand);
    }

    private List<String> acceptedAliases(String primaryCommand, List<String> aliases, Command allowedCurrentCommand) {
        Set<String> accepted = new LinkedHashSet<String>();
        for (String alias : aliases) {
            if (alias.equals(primaryCommand)) {
                continue;
            }

            Command existing = this.knownCommands.get(alias);
            if (existing != null && existing != allowedCurrentCommand) {
                this.plugin.getLogger().warning("Command alias '/" + alias + "' is already registered by another plugin and was skipped.");
                continue;
            }

            accepted.add(alias);
        }
        return new ArrayList<String>(accepted);
    }

    private void unregisterCurrent() {
        if (this.registeredCommand == null) {
            return;
        }

        this.registeredCommand.unregister(this.commandMap);
        removeKnownCommandEntries(this.registeredCommand);
        this.registeredCommand = null;
        this.registeredLabels = new ArrayList<String>();
    }

    private void restorePrevious(DynamicPluginCommand previousCommand, List<String> previousLabels) {
        if (previousCommand == null) {
            return;
        }

        this.commandMap.register(this.plugin.getName().toLowerCase(Locale.ENGLISH), previousCommand);
        this.registeredCommand = previousCommand;
        this.registeredLabels = previousLabels;
    }

    private void removeKnownCommandEntries(Command command) {
        Iterator<Map.Entry<String, Command>> iterator = this.knownCommands.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Command> entry = iterator.next();
            if (entry.getValue() == command) {
                iterator.remove();
            }
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static final class RegisteredCommand {

        private final DynamicPluginCommand command;
        private final List<String> labels;

        private RegisteredCommand(DynamicPluginCommand command, List<String> labels) {
            this.command = command;
            this.labels = labels;
        }

        private DynamicPluginCommand command() {
            return this.command;
        }

        private List<String> labels() {
            return this.labels;
        }
    }
}
