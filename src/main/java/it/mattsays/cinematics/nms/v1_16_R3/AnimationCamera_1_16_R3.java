package it.mattsays.cinematics.nms.v1_16_R3;

import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.animations.AnimationCamera;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.inventivetalent.packetlistener.PacketListenerAPI;
import org.inventivetalent.packetlistener.handler.PacketHandler;
import org.inventivetalent.packetlistener.handler.ReceivedPacket;
import org.inventivetalent.packetlistener.handler.SentPacket;

import java.util.UUID;

public class AnimationCamera_1_16_R3 extends AnimationCamera {

    private EntityArmorStand armorStand;

    public AnimationCamera_1_16_R3(UUID id, Player player) {
        super(id, player);
    }

    @Override
    public void spawn(Location location) {
        this.armorStand = new EntityArmorStand(EntityTypes.ARMOR_STAND,
                ((CraftWorld)location.getWorld()).getHandle());

        this.teleportTo(location);
        this.needsUpdate = false;

        this.armorStand.setLocation(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());

        this.armorStand.setBasePlate(false);
        this.armorStand.setArms(false);
        this.armorStand.setNoGravity(true);
        this.armorStand.setInvisible(true);
        this.armorStand.setInvulnerable(true);

        var spawnPacket = new PacketPlayOutSpawnEntity(this.armorStand);
        var metadataPacket = new PacketPlayOutEntityMetadata(this.armorStand.getId(), this.armorStand.getDataWatcher(), false);
        var headRotationPacket = new PacketPlayOutEntityHeadRotation(this.armorStand, (byte) ((location.getYaw() / 360.0d) * 255.0d));

        var entityPlayer = ((CraftPlayer)this.destinationPlayer).getHandle();
        entityPlayer.playerConnection.sendPacket(spawnPacket);
        entityPlayer.playerConnection.sendPacket(metadataPacket);
        entityPlayer.playerConnection.sendPacket(headRotationPacket);

    }



    @Override
    public void update() {

        this.currentVelocity.add(this.currentAcceleration.clone().multiply(0.2));

        // Update position and rotation modified by velocity (0.2 = 1 Tick in second)
        this.move(this.getCurrentVelocity().clone().multiply(0.2));
        this.rotate(this.getCurrentRotationVelocity().clone().multiply(0.2));

        if(this.needsUpdate) {
            var position = this.getCurrentPosition();
            var rotation = this.getCurrentRotation();
            // Update rotation and position
            this.armorStand.setLocation(
                    position.getX(), position.getY(), position.getZ(),
                    0, (float)rotation.getY()
            );

            this.needsUpdate = false;

            // Send update to the player
            var teleportPacket = new PacketPlayOutEntityTeleport(this.armorStand);
            var headRotationPacket = new PacketPlayOutEntityHeadRotation(this.armorStand, (byte) ((rotation.getX() / 360.0d) * 255.0d));

            var entityPlayer = ((CraftPlayer)this.destinationPlayer).getHandle();
            entityPlayer.playerConnection.sendPacket(headRotationPacket);
            entityPlayer.playerConnection.sendPacket(teleportPacket);

        }

    }

    @Override
    public void destroy() {
        var killPacket = new PacketPlayOutEntityDestroy(this.armorStand.getId());
        ((CraftPlayer)this.destinationPlayer).getHandle().playerConnection.sendPacket(killPacket);
    }

    @Override
    public void setSpectator(boolean enable) {
        var cameraPacket = new PacketPlayOutCamera(enable ? this.armorStand : ((CraftPlayer)this.destinationPlayer).getHandle());
        ((CraftPlayer)this.destinationPlayer).getHandle().playerConnection.sendPacket(cameraPacket);
    }
}
