package it.mattsays.cinematics.animations;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.api.animations.Animations;
import it.mattsays.cinematics.utils.DataContainer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnimationsManager implements Animations {


    public static Path BASE_ANIMATIONS_PATH = Path.of("animations");
    private final Map<String, Animation> animations;
    private final Map<UUID, VisualizationData> animationVisualization;
    private final Map<UUID, String> playerAnimations;

    public AnimationsManager() {
        this.animations = new HashMap<>();
        this.animationVisualization = new HashMap<>();
        this.playerAnimations = new HashMap<>();
        BASE_ANIMATIONS_PATH.toFile().mkdirs();
    }

    public Map<UUID, String> getPlayerAnimations() {
        return playerAnimations;
    }

    public Optional<Animation> getAnimation(String name) {
        return Optional.ofNullable(this.animations.get(name));
    }

    public List<String> listAnimationNames() {
        return new ArrayList<>(this.animations.keySet());
    }

    public void unloadAnimations() {
        this.stopVisualizations();
        this.animations.values().forEach(Animation::stopAll);
        this.animations.clear();
    }

    public void registerAnimationsEvents() {
        this.animations.values().forEach(
                animation -> Bukkit.getPluginManager().registerEvents(animation, Cinematics.getInstance())
        );
    }

    public void loadAnimations() {

        try (Stream<Path> stream = Files.walk(BASE_ANIMATIONS_PATH, Integer.MAX_VALUE)) {
            List<Path> collect = stream
                    .filter(path -> path.toFile().isFile())
                    .sorted()
                    .collect(Collectors.toList());


            collect.forEach((filePath) -> {
                try {
                    var animationOptional = this.loadAnimation(Files.readString(filePath), filePath);
                    animationOptional.ifPresentOrElse(
                            animation -> this.animations.put(animation.getName(), animation),
                            () -> Cinematics.LOGGER.error("Couldn't load animation '" + filePath.getFileName() +
                                    "' in " + filePath.getParent().toString())
                    );

                } catch (Exception e) {
                    Cinematics.LOGGER.error("Couldn't load animation '" + filePath.getFileName() +
                            "' in " + filePath.getParent().toString() + ": ");
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            Cinematics.LOGGER.error("Couldn't load animations in " +
                    BASE_ANIMATIONS_PATH.toString() + ": " + e.getMessage());
        }
    }

    public void reloadAnimations() {
        this.unloadAnimations();
        this.loadAnimations();
    }

    public Optional<Animation> loadAnimation(String jsonData, Path path) throws JsonIOException {
        var jsonObject = new JsonParser().parse(jsonData).getAsJsonObject();

        var type = Animation.AnimationType.valueOf(jsonObject.get("type").getAsString());

        Animation finalAnimation = null;


        switch (type) {
            case CAMERA:
                finalAnimation = new CinematicAnimation(jsonObject, path);
                break;
            case SCENE:
                finalAnimation = new SceneAnimation(jsonObject, path);
                break;
        }

        return Optional.ofNullable(finalAnimation);
    }

    public boolean visualizeAnimation(String name, Player player) {

        var statusContainer = new DataContainer<>(false);

        this.getAnimation(name).ifPresent(animation -> {
            if (!(animation instanceof SceneAnimation sceneAnimation)) {
                return;
            }

            var actorsData = new HashMap<>(sceneAnimation.getAnimationActorsData());

            this.stopCurrentVisualization(player);

            var animationPoints = new HashMap<UUID, Location[]>();
            var actors = new AnimationActor[actorsData.size()];

            var index = 0;

            for (var actorData : actorsData.values()) {

                actorData.setLooping(true);

                int finalIndex = index;
                sceneAnimation.actorSpawn(player, actorData).ifPresent(actor -> {
                    actors[finalIndex] = actor;
                });

                animationPoints.put(actorData.getID(), actorData.getAnimationPoints());
                index++;
            }

            var greenDust = new Particle.DustOptions(Color.GREEN, 1);
            var redDust = new Particle.DustOptions(Color.RED, 1);

            var task = Bukkit.getScheduler().runTaskTimer(Cinematics.getInstance(), () -> {
                animationPoints.forEach((uuid, locations) -> {
                    var actorDust = new Particle.DustOptions(Color.fromRGB(Math.abs(uuid.hashCode()) % 0x1000000), 1);
                    for (int i = 0; i < locations.length; i++) {
                        Particle.DustOptions dustOption;

                        if (i == 0) {
                            dustOption = greenDust;
                        } else if (i == locations.length - 1) {
                            dustOption = redDust;
                        } else {
                            dustOption = actorDust;
                        }

                        player.spawnParticle(Particle.REDSTONE, locations[i], 20, dustOption);
                    }
                });
                for (var actor : actors) {
                    sceneAnimation.actorUpdate(actor, actorsData.get(actor.id));
                }
            }, 10L, 0L);

            this.animationVisualization.put(player.getUniqueId(), new VisualizationData(task, actors));

            statusContainer.data = true;
        });

        return statusContainer.data;
    }

    public void stopCurrentVisualization(Player player) {
        var visualizationData = this.animationVisualization.get(player.getUniqueId());

        if (visualizationData != null) {
            visualizationData.task.cancel();
            for (var actor : visualizationData.actors) {
                actor.destroy();
            }
            this.animationVisualization.remove(player.getUniqueId());
        }
    }

    private void stopVisualizations() {

        for (var visualizationData : this.animationVisualization.values()) {
            visualizationData.task.cancel();
            for (var actor : visualizationData.actors) {
                actor.destroy();
            }
        }

        this.animationVisualization.clear();
    }

    public boolean saveAnimation(Animation animation, @Nullable String authorName, @Nullable String saveDir) {
        var jsonObject = new JsonObject();

        // Base settings
        jsonObject.addProperty("name", animation.getName());
        jsonObject.addProperty("type", animation.getType().name());
        jsonObject.addProperty("author", authorName != null ? authorName : "Unknown");

        // Specific settings
        var result = animation.save(jsonObject);

        if (!result) {
            Cinematics.LOGGER.error("Unable to save animation '" + animation.getName() + "'");
            return false;
        }

        var gson = new GsonBuilder().setPrettyPrinting().create();

        var subPath = saveDir == null ? "" : saveDir;

        var dirFile = new File(BASE_ANIMATIONS_PATH.toString() + "/" + subPath);

        if (!dirFile.exists()) {
            dirFile.mkdir();
        }

        var jsonFilePath = Path.of(BASE_ANIMATIONS_PATH.toString(), subPath, animation.getName() + ".json");

        try (Writer writer = new FileWriter(jsonFilePath.toString())) {
            writer.write(gson.toJson(jsonObject));
        } catch (IOException e) {
            Cinematics.LOGGER.error("Couldn't save animation '" + animation.getName() +
                    "' in " + jsonFilePath.getParent().toString() + ": " + e.getMessage());
            return false;
        }

        this.animations.put(animation.getName(), animation);

        return true;
    }

    public boolean deleteAnimation(String animationName) {

        if (!this.animations.containsKey(animationName)) {
            Cinematics.LOGGER.error("Couldn't delete animation '" + animationName + "': " + "not found");
            return false;
        }

        var animation = this.animations.remove(animationName);

        try {
            Files.delete(animation.getPath());
        } catch (IOException e) {
            Cinematics.LOGGER.error("Couldn't delete animation '" + animationName + "': " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean playAnimation(String name, Player player) {
        this.stopCurrentVisualization(player);
        var status = new DataContainer<>(false);
        this.getAnimation(name).ifPresent(animation -> {
            animation.play(player);
            status.data = true;
        });
        return status.data;
    }

    @Override
    public boolean stopAnimation(String name, Player player) {
        var status = new DataContainer<>(false);
        this.getAnimation(name).ifPresent(animation ->  {
            animation.stop(player);
            status.data = true;
        });
        return status.data;
    }

    public boolean isRunningAnimation(Player player) {
        return this.playerAnimations.containsKey(player.getUniqueId());
    }

    public boolean stopAnimation(Player player) {
        if (this.isRunningAnimation(player)) {
            var name = this.playerAnimations.get(player.getUniqueId());
            return this.stopAnimation(name, player);
        } else {
            return false;
        }
    }

    private static final class VisualizationData {
        public BukkitTask task;
        public AnimationActor[] actors;

        public VisualizationData(BukkitTask task, AnimationActor[] actors) {
            this.task = task;
            this.actors = actors;
        }
    }

}
