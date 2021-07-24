package it.mattsays.cinematics.nms.v1_16_R3;

import it.mattsays.cinematics.animations.SpectateSettings;
import net.minecraft.server.v1_16_R3.PacketPlayOutGameStateChange;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class SpectateSettings_1_16_R3 implements SpectateSettings {


    @Override
    public void fakeGamemode(Player player) {
        var entityPlayer = ((CraftPlayer)player).getHandle();

        var gameModeChangePacket = new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.d, 3);
        entityPlayer.playerConnection.sendPacket(gameModeChangePacket);

    }
}
