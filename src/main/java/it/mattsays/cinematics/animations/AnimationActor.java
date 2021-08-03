package it.mattsays.cinematics.animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

public abstract class AnimationActor {

    protected final Player destinationPlayer;
    protected boolean stopped;
    protected UUID id;
    protected float speed;
    protected int currentAnimationPointIndex;

    public AnimationActor(UUID id, Player player) {
        this.id = id;
        this.destinationPlayer = player;
        this.currentAnimationPointIndex = 0;
    }

    public static Vector getRotationVector(Location location) {
        return new Vector(location.getYaw(), location.getPitch(), 0);
    }

    public abstract Location getCurrentLocation();

    public UUID getID() {
        return id;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public abstract void teleportTo(Location location);

    public abstract void init(BaseActorData actorData);

    public abstract void spawn(Location location);

    public abstract void logicUpdate(BaseActorData actorData);

    public abstract void update();

    public abstract void destroy();

    public abstract void remove();

    public static class BaseActorData {
        protected Location[] animationPoints;
        private UUID id;
        private Class<? extends AnimationActor> actor;
        private boolean dynamic, main, loop;
        private float speed;

        public BaseActorData() {
        }

        public BaseActorData(Class<? extends AnimationActor> actor, Location[] animationPoints, float speed) {

            this.id = UUID.randomUUID();
            this.actor = actor;
            this.animationPoints = animationPoints;
            this.dynamic = this.animationPoints.length > 1;
            this.speed = speed;
        }

        public BaseActorData(Class<? extends AnimationActor> actor, Location spawnPoint) {
            this.id = UUID.randomUUID();
            this.actor = actor;
            this.animationPoints = new Location[]{spawnPoint};
            this.dynamic = false;
        }

        public static Optional<? extends BaseActorData> createActorData(JsonObject jsonActor) {

            var type = jsonActor.get("type").getAsString();

            try {
                var clazz = (Class<? extends BaseActorData>) Class.forName(type);
                return Optional.of((BaseActorData) clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                Cinematics.LOGGER.error("Json animation loading > Invalid class type '" + type + "' for actor data");
                e.printStackTrace();
                return Optional.empty();
            }
        }

        public Class<? extends BaseActorData> getType() {
            return BaseActorData.class;
        }

        public boolean isMain() {
            return main;
        }

        public void setMain(boolean main) {
            this.main = main;
        }

        public boolean isLooping() {
            return loop;
        }

        public void setLooping(boolean loop) {
            this.loop = loop;
        }

        public boolean save(JsonObject jsonActor) {

            jsonActor.addProperty("id", this.id.toString());

            jsonActor.addProperty("type", this.getType().getName());

            jsonActor.addProperty("class", this.actor.getName());

            if (this.dynamic) {
                jsonActor.addProperty("loop", this.loop);
                jsonActor.addProperty("speed", this.speed);
            }

            var jsonLocations = new JsonArray();

            for (var animationPoint : this.animationPoints) {
                var jsonLocation = new JsonObject();

                jsonLocation.addProperty("x", animationPoint.getX());
                jsonLocation.addProperty("y", animationPoint.getY());
                jsonLocation.addProperty("z", animationPoint.getZ());
                jsonLocation.addProperty("yaw", animationPoint.getYaw());
                jsonLocation.addProperty("pitch", animationPoint.getPitch());

                jsonLocations.add(jsonLocation);
            }

            jsonActor.add("locations", jsonLocations);

            return true;
        }

        public boolean load(JsonObject jsonActor, World world) {

            this.id = UUID.fromString(jsonActor.get("id").getAsString());

            var type = jsonActor.get("class").getAsString();

            try {
                this.actor = (Class<? extends AnimationActor>) Class.forName(type);
            } catch (ClassNotFoundException e) {
                Cinematics.LOGGER.error("Json animation loading > Invalid class type for actor " + this.id.toString());
                return false;
            }

            var jsonAnimationPoints = jsonActor.getAsJsonArray("locations");

            this.animationPoints = new Location[jsonAnimationPoints.size()];

            for (int i = 0; i < jsonAnimationPoints.size(); i++) {
                var jsonAnimationPoint = jsonAnimationPoints.get(i).getAsJsonObject();
                var x = jsonAnimationPoint.get("x").getAsFloat();
                var y = jsonAnimationPoint.get("y").getAsFloat();
                var z = jsonAnimationPoint.get("z").getAsFloat();
                var yaw = jsonAnimationPoint.get("yaw").getAsFloat();
                var pitch = jsonAnimationPoint.get("pitch").getAsFloat();
                this.animationPoints[i] = new Location(world, x, y, z, yaw, pitch);
            }

            this.dynamic = this.animationPoints.length > 1;

            if (this.dynamic) {
                this.speed = jsonActor.get("speed").getAsFloat();
                this.loop = jsonActor.get("loop").getAsBoolean();
            }

            return true;
        }

        public UUID getID() {
            return id;
        }

        public boolean isDynamic() {
            return dynamic;
        }

        public Class<? extends AnimationActor> getActor() {
            return actor;
        }

        public Location getSpawnLocation() {
            return animationPoints[0];
        }

        public Location[] getAnimationPoints() {
            return animationPoints;
        }

        public float getSpeed() {
            return speed;
        }

    }

}
