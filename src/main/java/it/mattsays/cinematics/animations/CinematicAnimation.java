package it.mattsays.cinematics.animations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.nms.factories.AnimationActorsFactory;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

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
