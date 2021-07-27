package it.mattsays.cinematics.commands;

import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.animations.AnimationMob;
import it.mattsays.cinematics.animations.AnimationsManager;
import it.mattsays.cinematics.animations.SceneAnimation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.EnumUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class AnimationCreator extends PaperCommand {

    private Random random;
    private List<Location> locations;
    private EntityType type;

    private AnimationsManager animationsManager;

    public AnimationCreator() {
        super("animcreator");
        this.locations = new ArrayList<>();
        this.random = new Random();
        this.animationsManager = Cinematics.getInstance().getAnimationsManager();
        this.type = EntityType.CREEPER;
    }

    @Override
    protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
        return true;
    }

    @Override
    protected void executeCommand(CommandSender commandSender, String[] arguments) {
        if (arguments[0].equalsIgnoreCase("type")) {
            var name = arguments[1].toUpperCase();

            if (EnumUtils.isValidEnum(EntityType.class, name)) {
                this.type = EnumUtils.getEnum(EntityType.class, name);
                commandSender.sendMessage(ChatColor.GREEN + "Type set");
            } else {
                commandSender.sendMessage(ChatColor.RED + "Invalid type name");
            }

        }
        if (arguments[0].equalsIgnoreCase("add")) {

            var playerLoc = ((Entity) commandSender).getLocation().toBlockLocation().add(0.5, 0, 0.5);

            this.locations.add(playerLoc);
            commandSender.sendMessage(ChatColor.GREEN + "Position added");
        } else if (arguments[0].equalsIgnoreCase("create")) {
            var name = arguments[1];
            var animation = new SceneAnimation(name);
            var mobActor = new AnimationMob.MobActorData(this.type, locations.toArray(new Location[0]), 1f);
            mobActor.setMain(true);
            mobActor.setCanBeSpectated(false);
            animation.addAnimationActor(mobActor);
            var result = this.animationsManager.saveAnimation(animation, commandSender.getName(), null);
            if (result) {
                commandSender.sendMessage(ChatColor.GREEN + "Animation created successfully");
                this.locations.clear();
            } else {
                commandSender.sendMessage(ChatColor.RED + "There was an error creating animation");
            }
        } else if (arguments[0].equalsIgnoreCase("play")) {
            var animationName = arguments[1];
            this.animationsManager.playAnimation(animationName, (Player) commandSender);
            commandSender.sendMessage(ChatColor.GREEN + "Animation started");
        } else if (arguments[0].equalsIgnoreCase("stop")) {
            this.animationsManager.stopAnimation((Player) commandSender);
            commandSender.sendMessage(ChatColor.GREEN + "Animation stopped");
        } else if (arguments[0].equalsIgnoreCase("reload")) {
            this.animationsManager.unloadAnimations();
            this.animationsManager.loadAnimations();
            commandSender.sendMessage(ChatColor.GREEN + "Animations reloaded");
        } else if (arguments[0].equalsIgnoreCase("reset")) {
            this.locations.clear();
            commandSender.sendMessage(ChatColor.GREEN + "Animation points reset");
        } else if (arguments[0].equalsIgnoreCase("list")) {
            var animations = this.animationsManager.listAnimationNames().toString();
            commandSender.sendMessage("Animations: " + ChatColor.GREEN + animations);
        } else if (arguments[0].equalsIgnoreCase("delete")) {
            var animationName = arguments[1];

            if (this.animationsManager.deleteAnimation(animationName)) {
                commandSender.sendMessage(ChatColor.GREEN + "Animation deleted");
            } else {
                commandSender.sendMessage(ChatColor.RED + "Animation not deleted. See console for more info");
            }
        } else if (arguments[0].equalsIgnoreCase("visualize")) {
            var animationName = arguments[1];
            var player = ((Player) commandSender);

            if (animationName.equals("stop")) {
                this.animationsManager.stopCurrentVisualization(player);
                player.sendMessage(ChatColor.GREEN + "Animation visualization stopped!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, this.random.nextFloat());
            }

            if (this.animationsManager.visualizeAnimation(animationName, player)) {
                player.sendMessage(ChatColor.GREEN + "Animation visualized!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, this.random.nextFloat());
            }
        }
    }

    @Override
    protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
        return Optional.of(Arrays.asList("play", "stop", "add", "create", "delete", "visualize", "reload", "reset"));
    }

}
