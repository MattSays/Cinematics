package it.mattsays.cinematics.api.effects;

import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.commons.SpecialChars;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FadeEffect {

    private static final int TEN_MINUTES = 20 * 60 * 10;

    private static Map<UUID, GameMode> playerGameModes;

    static {
        playerGameModes = new HashMap<>();
    }

    public static void fadeIn(Player player, int ticks) {
        player.sendTitle(SpecialChars.black_screen, null, ticks, TEN_MINUTES, 0);
        playerGameModes.put(player.getUniqueId(), player.getGameMode());
        Bukkit.getScheduler().runTask(Cinematics.getInstance(),
                () -> player.setGameMode(GameMode.SPECTATOR));
    }

    public static void fadeOut(Player player, int ticks) {
        player.sendTitle(SpecialChars.black_screen, null, 0, 0, ticks);
        Bukkit.getScheduler().runTask(Cinematics.getInstance(), () -> {
            if(playerGameModes.containsKey(player.getUniqueId()))
                player.setGameMode(playerGameModes.get(player.getUniqueId()));
            playerGameModes.remove(player.getUniqueId());
        });
    }

    public static void fade(Player player, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(SpecialChars.black_screen, null, fadeIn, stay, fadeOut);
    }

}
