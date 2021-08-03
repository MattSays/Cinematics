package it.mattsays.cinematics.animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SceneAnimation extends Animation {

    private Map<UUID, AnimationActor.BaseActorData> animationActorsData;

    public SceneAnimation(JsonObject jsonObject, Path path) {
        super(jsonObject, path);
    }

    public SceneAnimation(String name) {
        super(name);
        this.animationActorsData = new HashMap<>();
    }

    public void setAnimationActors(List<AnimationActor.BaseActorData> animationActorsData) {
        UUID mainActor = null;
        for (var actorData : animationActorsData) {
            if (actorData.isMain()) {
                mainActor = actorData.getID();
            }
            this.animationActorsData.put(actorData.getID(), actorData);
        }

        if (mainActor != null)
            this.mainActor = mainActor;
        else
            Cinematics.LOGGER.warn("No main actor found in scene '" + this.getName() + "'");
    }

    public void addAnimationActor(AnimationActor.BaseActorData actorData) {
        this.animationActorsData.put(actorData.getID(), actorData);
    }

    public Map<UUID, AnimationActor.BaseActorData> getAnimationActorsData() {
        return animationActorsData;
    }


    @Override
    public @NotNull AnimationType getType() {
        return AnimationType.SCENE;
    }

    @Override
    protected boolean onInit(Player player, AnimationData animationData) {

        var actors = new AnimationActor[this.animationActorsData.size()];

        int i = 0;
        for (var actorData : this.animationActorsData.values()) {
            int index = i;
            this.actorSpawn(player, actorData).ifPresent((actor) -> {
                actors[index] = actor;

                if (actor instanceof Spectated spectated && spectated.canBeSpectated()) {
                    spectated.setSpectator(true);
                }

            });

            i++;
        }

        animationData.actors = actors;

        // Hide player to all players and vice versa
        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            player.hidePlayer(Cinematics.getInstance(), otherPlayer);
            otherPlayer.hidePlayer(Cinematics.getInstance(), player);
        });

        return true;
    }

    @Override
    protected void onUpdate(Player player, AnimationData animationData) {
        for (var actor : animationData.actors) {

            var actorData = this.animationActorsData.get(actor.id);

            if (!actorData.isDynamic())
                continue;

            if (actor.stopped) {
                if (this.mainActor.equals(actor.id)) {
                    Bukkit.getScheduler().runTask(Cinematics.getInstance(), () -> this.stop(player));
                    return;
                }
            } else {
                this.actorUpdate(actor, actorData);
            }

        }
    }

    @Override
    protected void onEnd(Player player, AnimationData animationData) {
        for (var actor : animationData.actors) {

            if (actor instanceof Spectated) {
                ((Spectated) actor).setSpectator(false);
            }

            this.actorRemove(actor);
        }
    }

    @Override
    public boolean save(JsonObject jsonObject) {

        var jsonActors = new JsonArray();

        var worldSet = false;

        UUID mainActor = null;

        for (var actorData : this.animationActorsData.values()) {

            if (!worldSet) {
                worldSet = true;
                jsonObject.addProperty("world", actorData.getSpawnLocation().getWorld().getUID().toString());
            }

            if (actorData.isMain()) {
                if (mainActor != null) {
                    Cinematics.LOGGER.warn("There are multiple main actors in scene " + this.getName() + ". Using last actor as the main one.");
                }
                mainActor = actorData.getID();
            }

            var jsonActor = new JsonObject();

            actorData.save(jsonActor);

            jsonActors.add(jsonActor);
        }

        if (mainActor == null) {
            Cinematics.LOGGER.error("There is no main actor in scene " + this.getName() + ". Abort.");
            return false;
        }

        jsonObject.add("actors", jsonActors);
        jsonObject.addProperty("mainActor", mainActor.toString());

        return true;
    }

    @Override
    public void load(JsonObject jsonObject) {

        var worldUUID = UUID.fromString(jsonObject.get("world").getAsString());
        var world = Bukkit.getWorld(worldUUID);

        this.mainActor = UUID.fromString(jsonObject.get("mainActor").getAsString());

        var jsonActors = jsonObject.getAsJsonArray("actors");

        this.animationActorsData = new HashMap<>();

        for (var jsonActor : jsonActors) {

            var jsonActorObject = jsonActor.getAsJsonObject();

            AnimationActor.BaseActorData.createActorData(jsonActorObject).ifPresent(actorData -> {
                if (actorData.load(jsonActorObject, world)) {
                    this.animationActorsData.put(actorData.getID(), actorData);
                }
            });
        }

    }
}
