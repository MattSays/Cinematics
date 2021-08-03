package it.mattsays.cinematics.animations;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public abstract class AnimationCamera extends AnimationActor implements Spectated {

    public static class CameraActorData extends BaseActorData {

        private Vector[] velocityVectors, rotationVelocityVectors;


        public CameraActorData() {
            super();
        }

        public CameraActorData(Location[] animationPoints, float speed) {
            super(AnimationCamera.class, animationPoints, speed);

            if (this.isDynamic()) {
                this.velocityVectors = new Vector[animationPoints.length - 1];
                this.rotationVelocityVectors = new Vector[animationPoints.length - 1];
                this.calculateVectors();
            }
        }

        public CameraActorData(Location spawnPoint) {
            super(AnimationCamera.class, spawnPoint);
        }

        @Override
        public Class<? extends BaseActorData> getType() {
            return CameraActorData.class;
        }

        private void calculateVectors() {

            this.velocityVectors = new Vector[animationPoints.length - 1];
            this.rotationVelocityVectors = new Vector[animationPoints.length - 1];

            for (int i = 0; i < animationPoints.length - 1; i++) {
                var startVec = this.animationPoints[i].toVector();
                var startRotationVec = AnimationActor.getRotationVector(this.animationPoints[i]);

                var endVec = this.animationPoints[i + 1].toVector();
                var endRotationVec = AnimationActor.getRotationVector(this.animationPoints[i + 1]);

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

        @Override
        public boolean load(JsonObject jsonActor, World world) {

            var status = super.load(jsonActor, world);

            if (this.isDynamic()) {
                this.velocityVectors = new Vector[animationPoints.length - 1];
                this.rotationVelocityVectors = new Vector[animationPoints.length - 1];
                this.calculateVectors();
            }

            return status;
        }

        public Vector[] getVelocityVectors() {
            return velocityVectors;
        }

        public Vector[] getRotationVelocityVectors() {
            return rotationVelocityVectors;
        }
    }

    private static final Vector VECTOR_ZERO = new Vector();

    protected Vector currentPosition;
    protected Vector currentRotation;
    protected Vector currentVelocity, currentRotationVelocity;
    protected Vector currentAcceleration;

    public AnimationCamera(UUID id, Player player) {
        super(id, player);
        this.currentPosition = new Vector();
        this.currentRotation = new Vector();
        this.currentVelocity = new Vector();
        this.currentAcceleration = new Vector();
        this.currentRotationVelocity = new Vector();

    }

    @Override
    public boolean canBeSpectated() {
        return true;
    }

    public Vector getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Vector newPosition) {
        if (newPosition.equals(currentPosition))
            return;

        this.currentPosition = newPosition;
    }

    public void move(Vector movement) {
        if (movement.equals(VECTOR_ZERO))
            return;

        this.currentPosition.add(movement);
    }

    public Vector getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(Vector newRotation) {
        if (newRotation.equals(currentRotation))
            return;

        this.currentRotation = newRotation;
    }

    public void setCurrentAcceleration(Vector currentAcceleration) {
        this.currentAcceleration = currentAcceleration;
    }

    public void rotate(Vector rotation) {
        if (rotation.equals(VECTOR_ZERO))
            return;

        this.currentRotation.add(rotation);
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

    @Override
    public void init(BaseActorData actorData) {
        var cameraData = (CameraActorData) actorData;

        // Set initial velocity
        this.setCurrentVelocity(cameraData.getVelocityVectors()[0]);

        // Set initial rotation velocity
        this.setCurrentRotationVelocity(cameraData.getRotationVelocityVectors()[0]);

        this.setSpeed(cameraData.getSpeed());
    }

    @Override
    public void logicUpdate(BaseActorData actorData) {

        var cameraData = (CameraActorData) actorData;

        var animationPoints = cameraData.getAnimationPoints();
        var velocityVectors = cameraData.getVelocityVectors();
        var rotationVelocityVectors = cameraData.getRotationVelocityVectors();

        if (this.getCurrentPosition().toBlockVector().distance(animationPoints[this.currentAnimationPointIndex].toVector()) <= 0.01f) {

            if (this.currentAnimationPointIndex >= animationPoints.length - 1) {
                if (!cameraData.isLooping()) {
                    this.stopped = true;
                    return;
                } else {
                    this.currentAnimationPointIndex = 0;
                }
            }

            this.teleportTo(animationPoints[this.currentAnimationPointIndex]);
            this.setCurrentVelocity(velocityVectors[this.currentAnimationPointIndex]);
            this.setCurrentRotationVelocity(rotationVelocityVectors[this.currentAnimationPointIndex]);
            this.currentAnimationPointIndex++;
        }
    }

    @Override
    public void remove() {

    }
}
