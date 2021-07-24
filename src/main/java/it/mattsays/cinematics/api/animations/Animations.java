package it.mattsays.cinematics.api.animations;

import org.bukkit.entity.Player;

public interface Animations {

    void playAnimation(String name, Player player);

    void stopAnimation(String name, Player player);

}
