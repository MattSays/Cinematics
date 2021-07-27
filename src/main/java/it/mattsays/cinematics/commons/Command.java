package it.mattsays.cinematics.commons;

import java.util.*;

public abstract class Command {

    private HashMap<String, Command> subCommands;

    public Command() {
        this.subCommands = new HashMap<>();
    }

    protected abstract Optional<List<String>> getSuggestions(Object commandSender, String[] arguments);

    protected abstract void execute(Object commandSender, String[] arguments);

    protected abstract boolean hasPermissions(Object commandSender, String[] arguments);

    protected void addSubCommand(String subCommandLabel, Command command) {
        this.subCommands.put(subCommandLabel, command);
    }

    protected void setHelpMessage(Message message) {
        this.addSubCommand("help", new Command() {
            @Override
            protected Optional<List<String>> getSuggestions(Object commandSender, String[] arguments) {
                return Optional.empty();
            }

            @Override
            protected void execute(Object commandSender, String[] arguments) {
                message.send(commandSender);
            }

            @Override
            protected boolean hasPermissions(Object commandSender, String[] arguments) {
                return true;
            }
        });
    }

    protected void handleCommands(Object commandSender, String[] arguments) {
        if (arguments.length < 1) {
            this.execute(commandSender, arguments);
            return;
        }

        var subCommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        var subCommand = subCommands.getOrDefault(arguments[0], this);

        if (subCommand != this)
            subCommand.handleCommands(commandSender, subCommandArgs);
        else
            this.execute(commandSender, arguments);
    }

    protected boolean handlePermissions(Object commandSender, String[] arguments) {
        if (!this.hasPermissions(commandSender, arguments))
            return false;

        if (arguments.length > 1) {
            var subCommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
            var subCommand = subCommands.getOrDefault(arguments[0], this);

            return subCommand.handlePermissions(commandSender, subCommandArgs);
        }

        return true;
    }

    protected List<String> handleSuggestions(Object commandSender, String[] arguments) {
        if (arguments.length <= 1) {
            var suggestions = this.getSuggestions(commandSender, arguments).orElse(new ArrayList<>());
            suggestions.addAll(this.subCommands.keySet());
            return suggestions;
        }

        var subCommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        var subCommand = subCommands.getOrDefault(arguments[0], this);

        if (subCommand != this)
            return subCommand.handleSuggestions(commandSender, subCommandArgs);
        else
            return this.getSuggestions(commandSender, arguments).orElse(new ArrayList<>());
    }

}

