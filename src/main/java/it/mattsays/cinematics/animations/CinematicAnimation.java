package it.mattsays.cinematics.animations;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;

public class CinematicAnimation extends SceneAnimation {


    private float[] perTransitionSpeed;

    public CinematicAnimation(JsonObject jsonObject, Path path) {
        super(jsonObject, path);
    }

    public CinematicAnimation(@NotNull String name, float globalSpeed, @NotNull Location[] animationPoints) {
        super(name);

        this.setAnimationActors(
                Collections.singletonList(
                        new AnimationActor.BaseActorData(AnimationCamera.class, animationPoints, globalSpeed)
                )
        );
    }

    @Override
    public @NotNull AnimationType getType() {
        return AnimationType.CAMERA;
    }

}
