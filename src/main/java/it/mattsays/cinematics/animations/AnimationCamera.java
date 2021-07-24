package it.mattsays.cinematics.animations;

import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class AnimationCamera extends AnimationActor implements Spectated {

    public AnimationCamera(UUID id, Player player) {
        super(id, player);
    }

    @Override
    public boolean canBeSpectated() {
        return true;
    }
}
