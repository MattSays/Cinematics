package it.mattsays.cinematics.commands;

import it.mattsays.cinematics.commons.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public abstract class PaperCommand extends Command implements TabExecutor {

    private String commandName;

    protected PaperCommand(String command) {
        this.commandName = command;
    }

    public PaperCommand() {
        this.commandName = "";
    }

    protected abstract boolean hasPermissions(CommandSender commandSender, String[] arguments);
    protected abstract void executeCommand(CommandSender commandSender, String[] arguments);
    protected abstract Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments);


    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(this.handlePermissions(sender, args))
            this.handleCommands(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase(this.commandName))
            return this.handleSuggestions(sender, args);
        else
            return null;
    }

    @Override
    protected Optional<List<String>> getSuggestions(Object commandSender, String[] args) {
        return getSuggestions((CommandSender)commandSender, args);
    }

    @Override
    protected void execute(Object commandSender, String[] args) {
        this.executeCommand((CommandSender) commandSender, args);
    }

    @Override
    protected boolean hasPermissions(Object commandSender, String[] args) {
        return this.hasPermissions((CommandSender) commandSender, args);
    }
}
