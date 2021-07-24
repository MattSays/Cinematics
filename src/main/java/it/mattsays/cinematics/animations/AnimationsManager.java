package it.mattsays.cinematics.animations;

import com.google.gson.*;
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

    private static final class BaseAnimationData {
        public String name;
        public Animation.AnimationType type;
        public float globalSpeed;
    }

    private static final class VisualizationData {
        public BukkitTask task;
        public AnimationActor[] actors;

        public VisualizationData(BukkitTask task, AnimationActor[] actors) {
            this.task = task;
            this.actors = actors;
        }
    }

    private final Map<String, Animation> animations;
    private final Map<UUID, VisualizationData> animationVisualization;

    public static Path BASE_ANIMATIONS_PATH = Path.of("animations");

    public AnimationsManager() {
        this.animations = new HashMap<>();
        this.animationVisualization = new HashMap<>();
        BASE_ANIMATIONS_PATH.toFile().mkdirs();
    }

    public Optional<Animation> getAnimation(String name) {
        return Optional.ofNullable(this.animations.get(name));
    }

    public List<String> listAnimationNames() {
        return new ArrayList<>(this.animations.keySet());
    }

    public void unloadAnimations() {
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

                } catch (IOException e) {
                    Cinematics.LOGGER.error("Couldn't load animation '" + filePath.getFileName() +
                            "' in " + filePath.getParent().toString() + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            Cinematics.LOGGER.error("Couldn't load animations in " +
                    BASE_ANIMATIONS_PATH.toString() + ": " + e.getMessage());
        }
    }

    public Optional<Animation> loadAnimation(String jsonData, Path path) {
        var jsonObject = new JsonParser().parse(jsonData).getAsJsonObject();

        var type = Animation.AnimationType.valueOf(jsonObject.get("type").getAsString());

        Animation finalAnimation;

        switch (type) {
            case CAMERA:
                finalAnimation = new CinematicAnimation(jsonObject, path);
                break;
            case SCENE:
                finalAnimation = new SceneAnimation(jsonObject, path);
                break;
            default:
                finalAnimation = null;
                break;
        }

        return Optional.ofNullable(finalAnimation);
    }

    public boolean visualizeAnimation(String name, Player player) {

        var statusContainer = new DataContainer<>(false);

        this.getAnimation(name).ifPresent(animation -> {
            if(!(animation instanceof SceneAnimation sceneAnimation)) {
                return;
            }

            var actorsData = sceneAnimation.getAnimationActorsData();

            this.stopCurrentVisualization(player);

            var animationPoints = new HashMap<UUID, Location[]>();
            var actors = new AnimationActor[actorsData.size()];

            var index = 0;

            for (var actorData: actorsData.values()) {

                int finalIndex = index;
                sceneAnimation.actorSpawn(player, actorData).ifPresent(actor -> {
                    actors[finalIndex] = actor;
                });

                animationPoints.put(actorData.getID(), actorData.getAnimationPoints());
                index++;
            }


            var task = Bukkit.getScheduler().runTaskTimer(Cinematics.getInstance(), () -> {
                animationPoints.forEach((uuid, locations) -> {
                    var dustOption = new Particle.DustOptions(Color.fromRGB(Math.abs(uuid.hashCode()) % 0x1000000), 1);
                    for (var animationPoint : locations) {
                        player.spawnParticle(Particle.REDSTONE, animationPoint, 20, dustOption);
                    }
                });
            }, 10L, 0L);

            this.animationVisualization.put(player.getUniqueId(), new VisualizationData(task, actors));

            Bukkit.getScheduler().runTaskLaterAsynchronously(Cinematics.getInstance(), () -> {
                        task.cancel();
                        for (var actor : actors) {
                            actor.destroy();
                        }
                        this.animationVisualization.remove(player.getUniqueId());
            },50 * 20L);

            statusContainer.data = true;
        });

        return statusContainer.data;
    }

    public void stopCurrentVisualization(Player player) {
        var visualizationData = this.animationVisualization.get(player.getUniqueId());

        if(visualizationData != null) {
            visualizationData.task.cancel();
            for (var actor : visualizationData.actors) {
                actor.destroy();
            }
            this.animationVisualization.remove(player.getUniqueId());
        }
    }

    public boolean saveAnimation(Animation animation, @Nullable String authorName, @Nullable String saveDir) {
        var jsonObject = new JsonObject();

        // Base settings
        jsonObject.addProperty("name", animation.getName());
        jsonObject.addProperty("type", animation.getType().name());
        jsonObject.addProperty("author", authorName != null ? authorName : "Unknown");

        // Specific settings
        var result = animation.save(jsonObject);

        if(!result) {
            Cinematics.LOGGER.error("Unable to save animation '" + animation.getName() + "'");
            return false;
        }

        var gson = new GsonBuilder().setPrettyPrinting().create();

        var subPath = saveDir == null ? "" : saveDir;

        var dirFile = new File(BASE_ANIMATIONS_PATH.toString() + "/" + subPath);

        if(!dirFile.exists()) {
            dirFile.mkdir();
        }

        var jsonFilePath = Path.of(BASE_ANIMATIONS_PATH.toString(), subPath, animation.getName() + ".json");

        try(Writer writer = new FileWriter(jsonFilePath.toString())) {
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

        if(!this.animations.containsKey(animationName)) {
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
    public void playAnimation(String name, Player player) {
        this.stopCurrentVisualization(player);
        this.getAnimation(name).ifPresent(animation -> animation.play(player));
    }

    @Override
    public void stopAnimation(String name, Player player) {
        this.getAnimation(name).ifPresent(animation -> animation.stop(player));
    }

}
