package it.mattsays.cinematics.nms.factories;

import it.mattsays.cinematics.animations.AnimationActor;
import it.mattsays.cinematics.animations.AnimationCamera;
import it.mattsays.cinematics.animations.AnimationMob;
import it.mattsays.cinematics.animations.SpectateSettings;
import it.mattsays.cinematics.nms.v1_16_R3.AnimationCamera_1_16_R3;
import it.mattsays.cinematics.nms.v1_16_R3.AnimationMob_1_16_R3;
import it.mattsays.cinematics.nms.v1_16_R3.SpectateSettings_1_16_R3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class AnimationActorsFactory {

    private static String VERSION = "Unknown";

    static {
        try {
            VERSION = Bukkit.getServer().getClass().getPackageName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException exception) {
            exception.printStackTrace();
        }
    }

    public static Optional<? extends AnimationActor> createAnimationActor(Player player, Class<? extends AnimationActor> actorClass, UUID id) {
        switch (VERSION) {
            case "v1_16_R3":
                return createAnimationActor_1_16_R3(player, actorClass, id);
            default:
                return Optional.empty();
        }
    }

    private static Optional<? extends AnimationActor> createAnimationActor_1_16_R3(Player player, Class<? extends AnimationActor> actorClass, UUID id) {
        if(actorClass == AnimationCamera.class) {
            return Optional.of(new AnimationCamera_1_16_R3(id, player));
        } else if(actorClass == AnimationMob.class) {
            return Optional.of(new AnimationMob_1_16_R3(id, player));
        }
        return Optional.empty();
    }

    public static void fakeGameMode(Player player) {
        createSpectateSettings().ifPresent(spectateSettings -> {
            spectateSettings.fakeGamemode(player);
        });
    }

    private static Optional<? extends SpectateSettings> createSpectateSettings() {
        switch (VERSION) {
            case "v1_16_R3":
                return Optional.of(new SpectateSettings_1_16_R3());
            default:
                return Optional.empty();
        }
    }

}
