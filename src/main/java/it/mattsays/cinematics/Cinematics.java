package it.mattsays.cinematics;

import it.mattsays.cinematics.animations.AnimationsManager;
import it.mattsays.cinematics.api.CinematicsApi;
import it.mattsays.cinematics.commands.AnimationCreator;
import it.mattsays.cinematics.commands.PaperCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class Cinematics extends JavaPlugin implements CinematicsApi {

    public static final class Permissions {
        public static final String PERMISSION_BASE = "cinematics.";
        private static final String ANIMATION_CREATOR = PERMISSION_BASE + "animation_creator";
    }

    private static Cinematics INSTANCE;

    public static Logger LOGGER;

    private AnimationsManager animationsManager;

    public static Cinematics getInstance() {
        return INSTANCE;
    }

    @Override
    public AnimationsManager getAnimationsManager() {
        return this.animationsManager;
    }

    private void registerCommand(String command, PaperCommand cmd, String permission) {
        var pluginCommand = this.getCommand(command);

        if (pluginCommand == null) {
            LOGGER.error("Error registering command '" + command + "'");
            return;
        }

        pluginCommand.setExecutor(cmd);
        pluginCommand.setTabCompleter(cmd);
        pluginCommand.setPermission(permission);
    }

    @Override
    public void onLoad() {
        INSTANCE = this;
        LOGGER = this.getSLF4JLogger();
    }

    @Override
    public void onEnable() {

        LOGGER.info("Enabling plugin...");

        this.animationsManager = new AnimationsManager();
        this.animationsManager.loadAnimations();
        this.animationsManager.registerAnimationsEvents();


        this.registerCommand("animcreator", new AnimationCreator(), Permissions.ANIMATION_CREATOR);

        LOGGER.info("Plugin enabled");

    }


    @Override
    public void onDisable() {
        this.animationsManager.unloadAnimations();
    }
}
