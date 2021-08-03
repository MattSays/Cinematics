package it.mattsays.cinematics.animations;

import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public abstract class AnimationMob extends AnimationActor implements Spectated {

    public static Map<UUID, AnimationMob> animationMobs;
    public static BukkitTask updateTask;

    public static void initUpdateTask() {
        animationMobs = new HashMap<>();
        updateTask = Bukkit.getScheduler().runTaskTimer(Cinematics.getInstance(), () -> {
            animationMobs.values().forEach(AnimationMob::tick);
        }, 0L, 1L);
    }

    public static void addMob(AnimationMob animationMob) {
        animationMobs.put(animationMob.id, animationMob);
    }

    public static void removeMob(AnimationMob animationMob) {
        animationMobs.remove(animationMob.id);
    }

    public static void stopUpdateTask() {
        updateTask.cancel();
        updateTask = null;
        animationMobs.clear();
    }

    public static class MobActorData extends BaseActorData {

        private EntityType mobType;
        private boolean canBeSpectated;

        public MobActorData() {
            super();
            this.canBeSpectated = false;
        }

        public MobActorData(EntityType mobType, Location[] animationPoints, float speed) {
            super(AnimationMob.class, animationPoints, speed);

            this.mobType = mobType;
        }

        @Override
        public Class<? extends BaseActorData> getType() {
            return MobActorData.class;
        }

        public EntityType getMobType() {
            return mobType;
        }

        public void setCanBeSpectated(boolean canBeSpectated) {
            this.canBeSpectated = canBeSpectated;
        }

        public boolean canBeSpectated() {
            return canBeSpectated;
        }

        @Override
        public boolean save(JsonObject jsonActor) {

            jsonActor.addProperty("mobType", mobType.name());
            jsonActor.addProperty("canBeSpectated", canBeSpectated);

            return super.save(jsonActor);

        }

        @Override
        public boolean load(JsonObject jsonActor, World world) {

            try {
                this.mobType = EntityType.valueOf(jsonActor.get("mobType").getAsString());
            } catch (IllegalArgumentException e) {
                Cinematics.LOGGER.error("Json animation loading > Invalid mob type for actor " + this.getID().toString());
                return false;
            }

            this.canBeSpectated = jsonActor.get("canBeSpectated").getAsBoolean();

            return super.load(jsonActor, world);
        }
    }

    protected Location currentDestinationPoint;

    protected boolean canBeSpectated;

    protected EntityType mobType;

    public AnimationMob(UUID id, Player player) {
        super(id, player);
        this.canBeSpectated = false;
    }

    @Override
    public void init(BaseActorData actorData) {

        MobActorData mobActorData = (MobActorData) actorData;

        if (actorData.isDynamic()) {
            this.currentDestinationPoint = actorData.getAnimationPoints()[1];
            this.speed = mobActorData.getSpeed();
        }

        this.canBeSpectated = mobActorData.canBeSpectated;
        this.mobType = mobActorData.mobType;

        AnimationMob.addMob(this);

    }

    @Override
    public void logicUpdate(BaseActorData actorData) {

        if (this.isArrivedAtDestination()) {

            if (this.currentAnimationPointIndex + 1 >= actorData.animationPoints.length) {
                if (!actorData.isLooping()) {
                    this.stopped = true;
                    return;
                } else {
                    this.currentAnimationPointIndex = 0;
                    this.teleportTo(actorData.getSpawnLocation());
                    return;
                }
            }

            this.currentAnimationPointIndex++;
            this.setDestinationPoint(actorData.animationPoints[this.currentAnimationPointIndex]);
        }

    }

    @Override
    public void remove() {
        AnimationMob.removeMob(this);
    }

    public abstract void tick();

    @Override
    public boolean canBeSpectated() {
        return this.canBeSpectated;
    }

    public abstract boolean isArrivedAtDestination();

    public abstract void setDestinationPoint(Location location);
}
