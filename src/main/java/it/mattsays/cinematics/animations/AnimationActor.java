package it.mattsays.cinematics.animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

public abstract class AnimationActor {

    public static class BaseActorData {
        private final UUID id;
        private Class<? extends AnimationActor> actor;
        private boolean dynamic;
        protected Location[] animationPoints;

        private float speed;
        private Vector[] velocityVectors, rotationVelocityVectors;


        public BaseActorData() {
            this.id = UUID.randomUUID();
        }

        public Class<? extends BaseActorData> getType() {
            return BaseActorData.class;
        }

        public BaseActorData(Class<? extends AnimationActor> actor, Location[] animationPoints, float speed) {

            this.id = UUID.randomUUID();
            this.actor = actor;
            this.animationPoints = animationPoints;

            this.dynamic = this.animationPoints.length > 1;

            if(this.dynamic) {
                this.speed = speed;
                this.velocityVectors = new Vector[animationPoints.length - 1];
                this.rotationVelocityVectors = new Vector[animationPoints.length - 1];

                this.calculateVectors();
            }
        }

        protected void calculateVectors() {
            for (int i = 0; i < animationPoints.length - 1; i++) {
                var startVec = this.animationPoints[i].toVector();
                var startRotationVec = AnimationActor.getRotationVector(this.animationPoints[i]);

                var endVec = this.animationPoints[i+1].toVector();
                var endRotationVec = AnimationActor.getRotationVector(this.animationPoints[i+1]);

                var velocityVector = endVec
                        .subtract(startVec)
                        .normalize();

                var rotationVelocityVector = endRotationVec
                        .subtract(startRotationVec)
                        .normalize();

                this.velocityVectors[i] = velocityVector;
                this.rotationVelocityVectors[i] = rotationVelocityVector;
            }
        }

        public boolean save(JsonObject jsonActor) {

            jsonActor.addProperty("type", this.getType().getName());

            jsonActor.addProperty("class", this.actor.getName());

            var jsonLocations = new JsonArray();

            jsonActor.addProperty("speed", this.speed);

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

            if(this.dynamic) {
                this.speed = jsonActor.get("speed").getAsFloat();
                this.velocityVectors = new Vector[animationPoints.length - 1];
                this.rotationVelocityVectors = new Vector[animationPoints.length - 1];

                this.calculateVectors();
            }

            return true;
        }

        public static Optional<? extends BaseActorData> createActorData(JsonObject jsonActor) {

            var type = jsonActor.get("type").getAsString();

            try {
                var clazz = (Class<? extends BaseActorData>) Class.forName(type);
                return Optional.of((BaseActorData) clazz.getConstructors()[0].newInstance());
            } catch (Exception e) {
                Cinematics.LOGGER.error("Json animation loading > Invalid class type '" + type + "' for actor data");
                e.printStackTrace();
                return Optional.empty();
            }
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

        public Vector[] getVelocityVectors() {
            return velocityVectors;
        }

        public Vector[] getRotationVelocityVectors() {
            return rotationVelocityVectors;
        }
    }

    public static Vector VECTOR_ZERO = new Vector();

    protected boolean needsUpdate;
    protected UUID id;
    protected final Player destinationPlayer;
    protected Vector currentPosition, lastPosition;
    protected Vector currentRotation;
    protected Vector currentVelocity, currentRotationVelocity;
    protected Vector currentAcceleration;
    protected Location currentDestinationPoint;

    protected int currentAnimationPointIndex;

    public AnimationActor(UUID id, Player player) {
        this.id = id;
        this.currentPosition = new Vector();
        this.currentRotation = new Vector();
        this.currentVelocity = new Vector();
        this.currentAcceleration = new Vector();
        this.currentRotationVelocity = new Vector();
        this.destinationPlayer = player;
        this.needsUpdate = false;
        this.currentAnimationPointIndex = 0;
    }


    public Vector getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Vector newPosition) {
        if(newPosition.equals(currentPosition))
            return;

        this.currentPosition = newPosition;
        this.needsUpdate = true;
    }

    public void init(BaseActorData actorData) {

    }

    public void move(Vector movement) {
        if(movement.equals(VECTOR_ZERO))
            return;

        this.lastPosition = this.currentPosition;
        this.currentPosition.add(movement);

        this.needsUpdate = true;
    }

    public Vector getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(Vector newRotation) {
        if(newRotation.equals(currentRotation))
            return;

        this.currentRotation = newRotation;
        this.needsUpdate = true;
    }

    public void setCurrentAcceleration(Vector currentAcceleration) {
        this.currentAcceleration = currentAcceleration;
    }

    public void rotate(Vector rotation) {
        if(rotation.equals(VECTOR_ZERO))
            return;

        this.currentRotation.add(rotation);
        this.needsUpdate = true;
    }

    public Vector getCurrentVelocity() {
        return currentVelocity;
    }

    public void setCurrentVelocity(Vector currentVelocity) {
        this.currentVelocity = currentVelocity;
    }

    public Vector getCurrentRotationVelocity() {
        return currentRotationVelocity;
    }

    public void setCurrentRotationVelocity(Vector currentRotationVelocity) {
        this.currentRotationVelocity = currentRotationVelocity;
    }

    public void teleportTo(Location location) {
        if(location.toVector().equals(this.currentPosition) &&
                this.currentRotation.equals(getRotationVector(location)))
            return;

        this.currentPosition = location.toVector();
        this.currentRotation = getRotationVector(location);
        this.needsUpdate = true;
    }

    public UUID getID() {
        return id;
    }

    public abstract void spawn(Location location);
    public abstract void update();
    public abstract void destroy();

    public static Vector getRotationVector(Location location) {
        return new Vector(location.getYaw(), location.getPitch(), 0);
    }

}
