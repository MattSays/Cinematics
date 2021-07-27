package it.mattsays.cinematics.animations;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.api.effects.FadeEffect;
import it.mattsays.cinematics.nms.factories.AnimationActorsFactory;
import it.mattsays.cinematics.utils.DataContainer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

public abstract class Animation implements Listener {

    public enum AnimationType {
        CAMERA, SCENE
    }

    public static class AnimationData {
        public BukkitTask task;
        public Location initialPosition;
        public GameMode previousGameMode;
        public boolean wasFlying;
        public boolean allowedToFly;
        public boolean running;

        public ItemStack[] inventoryContents;
        public ItemStack[] armorContents;

        public AnimationActor[] actors;
    }

    private final String name;

    protected Map<UUID, AnimationData> playersAnimationData;

    protected UUID mainActor;

    private Path path;

    private AnimationsManager animationsManager = Cinematics.getInstance().getAnimationsManager();

    public Animation(String name) {
        this.name = name;
        this.playersAnimationData = new HashMap<>();
    }

    public Animation(JsonObject jsonObject, Path path) {
        this.name = jsonObject.get("name").getAsString();
        this.playersAnimationData = new HashMap<>();
        this.path = path;
        this.load(jsonObject);
    }


    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public boolean isAnimationRunning(UUID player) {
        return this.playersAnimationData.containsKey(player) && this.playersAnimationData.get(player).running;
    }


    public abstract @NotNull AnimationType getType();

    private void onPlayerPreInit(Player player) {
        // Clear inventory
        player.getInventory().clear();

        // Set player invisibility
        player.setInvisible(true);

        // Set player to spectator
        player.setGameMode(GameMode.ADVENTURE);

        // Set flight
        player.setAllowFlight(true);
        player.setFlying(true);

        AnimationActorsFactory.fakeGameMode(player);
    }

    private void onPlayerPostEnd(Player player, AnimationData animationData) {
        // Set previous game mode
        player.setGameMode(animationData.previousGameMode);

        // Set flight
        player.setFlying(animationData.wasFlying);
        player.setAllowFlight(animationData.allowedToFly);

        // Set player visibility
        player.setInvisible(false);

        // Reset initial inventory
        player.getInventory().setContents(animationData.inventoryContents);
        player.getInventory().setArmorContents(animationData.armorContents);

        // Teleport to initial position
        player.teleportAsync(animationData.initialPosition);

    }

    protected abstract boolean onInit(Player player, AnimationData animationData);

    protected abstract void onUpdate(Player player, AnimationData animationData);

    protected abstract void onEnd(Player player, AnimationData animationData);

    public Optional<AnimationActor> actorSpawn(Player player, AnimationActor.BaseActorData actorData) {

        var actorContainer = new DataContainer<AnimationActor>(null);

        AnimationActorsFactory.createAnimationActor(player, actorData.getActor(), actorData.getID())
                .ifPresent(animationActor -> {

                    actorContainer.data = animationActor;

                });

        var actor = actorContainer.data;

        if (actor == null) {
            return Optional.empty();
        }

        actor.init(actorData);

        actor.spawn(actorData.getSpawnLocation());

        return Optional.of(actor);
    }

    public void actorUpdate(AnimationActor actor, AnimationActor.BaseActorData actorData) {
        actor.logicUpdate(actorData);
        actor.update();
    }

    public void play(Player player) {

        if (this.isAnimationRunning(player.getUniqueId()))
            return;

        var animationData = new AnimationData();

        animationData.wasFlying = player.isFlying();
        animationData.allowedToFly = player.getAllowFlight();
        animationData.previousGameMode = player.getGameMode();
        animationData.initialPosition = player.getLocation();
        animationData.inventoryContents = player.getInventory().getContents();
        animationData.armorContents = player.getInventory().getArmorContents();
        animationData.running = true;

        // Send fade effect to player
        FadeEffect.fade(player, (int) (0.2 * 20), (int) (0.5 * 20), (int) (0.2 * 20));

        Bukkit.getScheduler().runTaskLater(Cinematics.getInstance(), () -> {
            this.onPlayerPreInit(player);
            if (this.onInit(player, animationData)) {
                // Start animation task
                animationData.task = Bukkit.getScheduler().runTaskTimerAsynchronously(Cinematics.getInstance(),
                        () -> this.onUpdate(player, animationData), (long) (0.8 * 20L), 1L);
            }
        }, (long) (0.2 * 20L));

        this.playersAnimationData.put(player.getUniqueId(), animationData);
        this.animationsManager.getPlayerAnimations().put(player.getUniqueId(), this.name);
    }

    public void stop(Player player) {

        if (!this.isAnimationRunning(player.getUniqueId()))
            return;

        var animationData = this.playersAnimationData.get(player.getUniqueId());

        animationData.task.cancel();

        // Send fade effect to player
        FadeEffect.fade(player, (int) (0.2 * 20), (int) (0.5 * 20), (int) (0.2 * 20));

        Bukkit.getScheduler().runTaskLater(Cinematics.getInstance(),
                () -> {
                    this.onPlayerPostEnd(player, animationData);
                    this.onEnd(player, animationData);
                }, (long) (0.2 * 20));

        this.playersAnimationData.remove(player.getUniqueId());
        this.animationsManager.getPlayerAnimations().remove(player.getUniqueId());
    }

    public void instantStop(Player player) {
        var animationData = this.playersAnimationData.get(player.getUniqueId());

        animationData.task.cancel();

        this.onPlayerPostEnd(player, animationData);
        this.onEnd(player, animationData);

        this.animationsManager.getPlayerAnimations().remove(player.getUniqueId());
        this.playersAnimationData.remove(player.getUniqueId());
    }

    public void stopAll() {
        var players = new ArrayList<Player>();
        this.playersAnimationData.keySet().forEach((uuid -> players.add(Bukkit.getPlayer(uuid))));
        players.forEach(this::instantStop);
    }

    public abstract boolean save(JsonObject jsonObject);

    public abstract void load(JsonObject jsonObject) throws JsonIOException;

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        var player = event.getPlayer();

        if (this.isAnimationRunning(player.getUniqueId()) && player.isSneaking()) {
            this.stop(player);
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (this.isAnimationRunning(player.getUniqueId())) {
            this.instantStop(player);
        }
    }


}
