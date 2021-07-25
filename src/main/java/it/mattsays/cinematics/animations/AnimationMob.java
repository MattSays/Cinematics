package it.mattsays.cinematics.animations;

import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public abstract class AnimationMob extends AnimationActor implements Spectated {

    public static class MobActorData extends BaseActorData {

        private EntityType mobType;
        private boolean canBeSpectated;

        public MobActorData() {
            super();
            this.canBeSpectated = false;
        }

        @Override
        public Class<? extends BaseActorData> getType() {
            return MobActorData.class;
        }

        public MobActorData(EntityType mobType, Location[] animationPoints, float speed, boolean canBeSpectated) {
            super(AnimationMob.class, animationPoints, speed);

            this.mobType = mobType;
            this.canBeSpectated = canBeSpectated;
        }

        private void optimizeAnimationPoints() {

            var animationPoints = new ArrayList<Location>();

            for (int i = 0; i < this.animationPoints.length - 1; i++) {
                var firstLoc = this.animationPoints[i];
                var secondLoc = this.animationPoints[i + 1];

                var yDiff = secondLoc.getBlockY() - firstLoc.getBlockY();

                animationPoints.add(firstLoc);

                if(yDiff != 0) {
                    if(Math.abs(yDiff) == 1) {

                        var startLoc = firstLoc.clone();
                        var directionVec = secondLoc.clone().subtract(firstLoc)
                                .toVector().setY(0).normalize();

                        var foundBlock = false;
                        Location jumpLocation = null;

                        while (!foundBlock) {

                            var stepLocation = startLoc.add(directionVec);

                            if(stepLocation.getBlock().isSolid()) {
                                foundBlock = true;
                                jumpLocation = stepLocation.add(0, yDiff, 0);
                            }

                            var distanceVecToDestination = secondLoc.clone().subtract(stepLocation).toVector();

                            if(distanceVecToDestination.getX() == 0
                                && distanceVecToDestination.getZ() == 0
                                && distanceVecToDestination.getY() == yDiff) {
                                break;
                            }

                        }

                        if(foundBlock) {
                            animationPoints.add(jumpLocation.clone().add(-directionVec.getX(), -yDiff, -directionVec.getZ()).toBlockLocation());
                            animationPoints.add(jumpLocation.toBlockLocation());
                        }

                    } else {
                        Cinematics.LOGGER.error("Y difference between location to high!");
                        Cinematics.LOGGER.error("\tFirst location > " + firstLoc);
                        Cinematics.LOGGER.error("\tSecond location > " + secondLoc);
                    }
                }

                if(i == this.animationPoints.length - 2) {
                    animationPoints.add(secondLoc);
                }
            }

            this.animationPoints = animationPoints.toArray(new Location[] {});
        }

        public EntityType getMobType() {
            return mobType;
        }

        public boolean canBeSpectated() {
            return canBeSpectated;
        }

        @Override
        public boolean save(JsonObject jsonActor) {

            if(this.isDynamic()) {
                this.optimizeAnimationPoints();
                this.calculateVectors();
            }

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

    protected boolean canBeSpectated;

    protected EntityType mobType;

    public AnimationMob(UUID id, Player player) {
        super(id, player);
        this.canBeSpectated = false;
    }

    @Override
    public void init(BaseActorData actorData) {

        MobActorData mobActorData = (MobActorData) actorData;

        this.canBeSpectated = mobActorData.canBeSpectated;

        this.mobType = mobActorData.mobType;
    }

    @Override
    public boolean canBeSpectated() {
        return this.canBeSpectated;
    }
}
