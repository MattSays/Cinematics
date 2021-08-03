package it.mattsays.cinematics.commands;

import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.animations.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.EnumUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Animations extends PaperCommand {

    private final static class AnimationSetupData {
        public String name;
        public List<AnimationActor.BaseActorData> actorData;
        public Animation.AnimationType type;
    }

    private List<Location> locations;
    private EntityType type;

    protected AnimationsManager animationsManager;

    private final static class Play extends PaperCommand {

        private final Animations animations;

        private Play(Animations animations) {
            super("play");
            this.animations = animations;
        }

        @Override
        protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
            return commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_PLAY);
        }

        @Override
        protected void executeCommand(CommandSender commandSender, String[] arguments) {
            var srcPlayer = (Player) commandSender;
            var animationName = arguments.length >= 1 ? arguments[0] : "";

            if(animationName.isEmpty()) {
                return;
            }


            var otherPlayerName = arguments.length == 2 ? arguments[1] : "";

            var destinationPlayer = srcPlayer;

            if(!otherPlayerName.isEmpty()) {
                if(srcPlayer.hasPermission(Cinematics.Permissions.ANIMATIONS_PLAY_OTHERS)) {
                    var otherPlayer = Bukkit.getPlayer(otherPlayerName);
                    destinationPlayer = otherPlayer != null ? otherPlayer : srcPlayer;
                } else {
                    srcPlayer.sendMessage(ChatColor.RED + "Insufficient permissions!");
                    return;
                }
            }

            var status = animations.animationsManager.playAnimation(animationName, destinationPlayer);

            if(status) {
                if(!destinationPlayer.getUniqueId().equals(srcPlayer.getUniqueId())) {
                    srcPlayer.sendMessage(ChatColor.GREEN + "Animation started for player " + ChatColor.AQUA + destinationPlayer.getName());
                }

                destinationPlayer.sendMessage(ChatColor.GREEN + "Animation started");
            } else {
                if(!destinationPlayer.getUniqueId().equals(srcPlayer.getUniqueId())) {
                    srcPlayer.sendMessage(ChatColor.RED + "Animation start error for player " + ChatColor.AQUA + destinationPlayer.getName());
                }
            }
        }

        @Override
        protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
            if(arguments.length <= 1 && commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_LIST)) {
                var animationName = arguments[0];
                var animationNames = this.animations.animationsManager.listAnimationNames();
                if(animationName.isEmpty()) {
                    return Optional.of(animationNames);
                } else {
                    var filteredNames = animationNames
                            .stream()
                            .filter(animation -> animation.toLowerCase().startsWith(animationName.toLowerCase()))
                            .collect(Collectors.toList());
                    return Optional.of(filteredNames);
                }
            } else if(arguments.length == 2 && commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_PLAY_OTHERS)) {
                var playerName = arguments[1];
                var playerNames = Bukkit.getOnlinePlayers()
                        .stream()
                        .map(HumanEntity::getName)
                        .collect(Collectors.toList());

                if(playerName.isEmpty()) {
                    return Optional.of(playerNames);
                } else {
                    var filteredNames = playerNames
                            .stream()
                            .filter(player -> player.toLowerCase().startsWith(playerName.toLowerCase()))
                            .collect(Collectors.toList());
                    return Optional.of(filteredNames);
                }
            } else {
                return Optional.empty();
            }
        }

    }

    private final static class Visualize extends PaperCommand {

        private final Animations animations;
        private final Random random;

        private Visualize(Animations animations) {
            super("visualize");
            this.animations = animations;
            this.random = new Random();
        }

        @Override
        protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
            return commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_VISUALIZE);
        }

        @Override
        protected void executeCommand(CommandSender commandSender, String[] arguments) {
            var player = (Player) commandSender;
            var animationName = arguments.length >= 1 ? arguments[0] : "";

            if(animationName.isEmpty()) {
                return;
            }

            var status = false;

            if(animationName.equals("stop")) {
                animations.animationsManager.stopCurrentVisualization(player);
                status = true;
            } else {
                status = animations.animationsManager.visualizeAnimation(animationName, player);
            }


            if(status) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, this.random.nextFloat());
                if(animationName.equals("stop")) {
                    player.sendMessage(ChatColor.GREEN + "Animation visualization stopped");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Animation visualization started");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Animation visualization error");
            }
        }

        @Override
        protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
            if(arguments.length <= 1 && commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_LIST)) {
                var animationName = arguments[0];
                var animationNames = this.animations.animationsManager.listAnimationNames();
                animationNames.add("stop");
                if(animationName.isEmpty()) {
                    return Optional.of(animationNames);
                } else {
                    var filteredNames = animationNames
                            .stream()
                            .filter(animation -> animation.toLowerCase().startsWith(animationName.toLowerCase()))
                            .collect(Collectors.toList());
                    return Optional.of(filteredNames);
                }
            } else {
                return Optional.empty();
            }
        }

    }

    private final static class Stop extends PaperCommand {

        private final Animations animations;

        private Stop(Animations animations) {
            super("stop");
            this.animations = animations;
        }

        @Override
        protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
            return commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_STOP);
        }

        @Override
        protected void executeCommand(CommandSender commandSender, String[] arguments) {
            var srcPlayer = (Player) commandSender;
            var otherPlayerName = arguments.length == 1 ? arguments[0] : "";

            var destinationPlayer = srcPlayer;

            if(!otherPlayerName.isEmpty()) {
                if(srcPlayer.hasPermission(Cinematics.Permissions.ANIMATIONS_STOP_OTHERS)) {
                    var otherPlayer = Bukkit.getPlayer(otherPlayerName);
                    destinationPlayer = otherPlayer != null ? otherPlayer : srcPlayer;
                } else {
                    srcPlayer.sendMessage(ChatColor.RED + "Insufficient permissions!");
                    return;
                }
            }

            var status = animations.animationsManager.stopAnimation(destinationPlayer);

            if(status) {

                if(!destinationPlayer.getUniqueId().equals(srcPlayer.getUniqueId())) {
                    srcPlayer.sendMessage(ChatColor.GREEN + "Animation stopped for player " + ChatColor.AQUA + destinationPlayer.getName());
                }

                destinationPlayer.sendMessage(ChatColor.GREEN + "Animation stopped");
            } else {
                if(!destinationPlayer.getUniqueId().equals(srcPlayer.getUniqueId())) {
                    srcPlayer.sendMessage(ChatColor.RED + "Animation stop error for player " + ChatColor.AQUA + destinationPlayer.getName());
                }
            }
        }

        @Override
        protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
            var playerName = arguments.length <= 1 ? arguments[0] : "";

            if(playerName.isEmpty()) {
                return Optional.empty();
            }

            var playerNames = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(HumanEntity::getName)
                    .collect(Collectors.toList());

            return Optional.of(playerNames.stream().filter(player -> player.toLowerCase().startsWith(playerName.toLowerCase())).collect(Collectors.toList()));
        }

    }

    private final static class ShowList extends PaperCommand {

        private final Animations animations;

        private ShowList(Animations animations) {
            super("list");
            this.animations = animations;
        }

        @Override
        protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
            return commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_LIST);
        }

        @Override
        protected void executeCommand(CommandSender commandSender, String[] arguments) {
            var animationNames = String.join(", ",
                    this.animations.animationsManager.listAnimationNames());

            commandSender.sendMessage(ChatColor.BLUE + "Animations: " + ChatColor.GREEN + animationNames);
        }

        @Override
        protected Optional<java.util.List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
            return Optional.empty();
        }
    }

    private final static class Reload extends PaperCommand {

        private final Animations animations;

        private Reload(Animations animations) {
            super("reload");
            this.animations = animations;
        }

        @Override
        protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
            return commandSender.hasPermission(Cinematics.Permissions.ANIMATIONS_RELOAD);
        }

        @Override
        protected void executeCommand(CommandSender commandSender, String[] arguments) {
            this.animations.animationsManager.loadAnimations();
            commandSender.sendMessage(ChatColor.GREEN + "Animations reloaded");
        }

        @Override
        protected Optional<java.util.List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
            return Optional.empty();
        }
    }

    public Animations(AnimationsManager animationsManager) {
        super("animations");
        this.locations = new ArrayList<>();
        this.animationsManager = animationsManager;
        this.type = EntityType.CREEPER;

        this.addSubCommand("play", new Play(this));
        this.addSubCommand("visualize", new Visualize(this));
        this.addSubCommand("stop", new Stop(this));
        this.addSubCommand("list", new ShowList(this));
        this.addSubCommand("reload", new Reload(this));
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
        } else if (arguments[0].equalsIgnoreCase("reset")) {
            this.locations.clear();
            commandSender.sendMessage(ChatColor.GREEN + "Animation points reset");
        } else if (arguments[0].equalsIgnoreCase("delete")) {
            var animationName = arguments[1];

            if (this.animationsManager.deleteAnimation(animationName)) {
                commandSender.sendMessage(ChatColor.GREEN + "Animation deleted");
            } else {
                commandSender.sendMessage(ChatColor.RED + "Animation not deleted. See console for more info");
            }
        }
    }

    @Override
    protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
        return Optional.empty();
    }

}
