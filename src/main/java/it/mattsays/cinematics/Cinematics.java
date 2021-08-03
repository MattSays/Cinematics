package it.mattsays.cinematics;

import it.mattsays.cinematics.animations.AnimationMob;
import it.mattsays.cinematics.animations.AnimationsManager;
import it.mattsays.cinematics.api.CinematicsApi;
import it.mattsays.cinematics.commands.Animations;
import it.mattsays.cinematics.commands.PaperCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class Cinematics extends JavaPlugin implements CinematicsApi {

    public static final class Permissions {
        public static final String PERMISSION_BASE = "cinematics.";
        public static final String ANIMATION_CREATOR = PERMISSION_BASE + "animation_creator";
        public static final String ANIMATIONS = PERMISSION_BASE + "animations";
        
        public static final String ANIMATIONS_PLAY = ANIMATIONS + ".play";
        public static final String ANIMATIONS_PLAY_OTHERS = ANIMATIONS_PLAY + ".others";

        public static final String ANIMATIONS_STOP = ANIMATIONS + ".stop";
        public static final String ANIMATIONS_STOP_OTHERS = ANIMATIONS_STOP + ".others";

        public static final String ANIMATIONS_LIST = ANIMATIONS + ".list";

        public static final String ANIMATIONS_RELOAD = ANIMATIONS + ".reload";

        public static final String ANIMATIONS_VISUALIZE = ANIMATIONS + ".visualize";

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

        AnimationMob.initUpdateTask();

        this.animationsManager = new AnimationsManager();
        this.animationsManager.loadAnimations();
        this.animationsManager.registerAnimationsEvents();


        this.registerCommand("animations", new Animations(this.animationsManager), Permissions.ANIMATION_CREATOR);

        LOGGER.info("Plugin enabled");

    }


    @Override
    public void onDisable() {
        this.animationsManager.unloadAnimations();
        AnimationMob.stopUpdateTask();
    }
}
