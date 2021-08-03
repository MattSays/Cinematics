package it.mattsays.cinematics.api.animations;

import org.bukkit.entity.Player;

public interface Animations {

    boolean playAnimation(String name, Player player);

    boolean stopAnimation(String name, Player player);

    boolean stopAnimation(Player player);
}
