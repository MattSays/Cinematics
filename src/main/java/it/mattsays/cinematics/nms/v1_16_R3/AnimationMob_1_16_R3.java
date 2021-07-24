package it.mattsays.cinematics.nms.v1_16_R3;

import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.animations.AnimationMob;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

public class AnimationMob_1_16_R3 extends AnimationMob {

    private EntityCreature actorMob;

    public AnimationMob_1_16_R3(UUID id, Player player) {
        super(id, player);
    }

    private Optional<? extends EntityCreature> getNMSCreature(EntityType mobType, World world) {
        var mobName = mobType.getEntityClass().getSimpleName();

        try {
            var mobClass = (Class<? extends EntityCreature>) Class.forName("net.minecraft.server.v1_16_R3.Entity" + mobName);
            var entityType = EntityTypes.class.getField(mobName.toUpperCase()).get(null);
            var mob = mobClass.getConstructor(EntityTypes.class, World.class).newInstance(entityType, world);
            return Optional.of(mob);
        } catch (Exception e) {
            Cinematics.LOGGER.error("Bukkit to NMS entity conversion error > " + e.getMessage());
            return Optional.empty();
        }


    }

    @Override
    public void spawn(Location location) {


        this.getNMSCreature(this.mobType, ((CraftWorld)location.getWorld()).getHandle()).ifPresent(mob -> {
            this.actorMob = mob;
        });

        this.actorMob.setLocation(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());

        this.teleportTo(location);
        this.needsUpdate = false;

        this.actorMob.setInvulnerable(true);

        var spawnPacket = new PacketPlayOutSpawnEntityLiving(this.actorMob);
        var metadataPacket = new PacketPlayOutEntityMetadata(this.actorMob.getId(), this.actorMob.getDataWatcher(), false);
        var headRotationPacket = new PacketPlayOutEntityHeadRotation(this.actorMob, (byte) ((location.getYaw() / 360.0d) * 255.0d));

        var entityPlayer = ((CraftPlayer)this.destinationPlayer).getHandle();

        entityPlayer.playerConnection.sendPacket(spawnPacket);
        entityPlayer.playerConnection.sendPacket(metadataPacket);
        entityPlayer.playerConnection.sendPacket(headRotationPacket);

    }

    @Override
    public void update() {
        this.currentVelocity.add(this.currentAcceleration.clone().multiply(0.2));


        // Update position and rotation modified by velocity (0.2 = 1 Tick in second)
        this.move(this.getCurrentVelocity().clone().multiply(0.02) );
        this.rotate(this.getCurrentRotationVelocity().clone().multiply(0.02));

        if(this.needsUpdate) {
            var position = this.getCurrentPosition();
            var rotation = this.getCurrentRotation();
            // Update rotation and position
            this.actorMob.setLocation(
                    position.getX(), position.getY(), position.getZ(),
                    0, (float)rotation.getY()
            );

            this.needsUpdate = false;

            // Send update to the player

            var teleportPacket = new PacketPlayOutEntityTeleport(this.actorMob);
            var headRotationPacket = new PacketPlayOutEntityHeadRotation(this.actorMob, (byte) ((rotation.getX() / 360.0d) * 255.0d));


            var entityPlayer = ((CraftPlayer)this.destinationPlayer).getHandle();

            entityPlayer.playerConnection.sendPacket(headRotationPacket);
            entityPlayer.playerConnection.sendPacket(teleportPacket);




        }
    }

    @Override
    public void destroy() {
        var killPacket = new PacketPlayOutEntityDestroy(this.actorMob.getId());
        ((CraftPlayer)this.destinationPlayer).getHandle().playerConnection.sendPacket(killPacket);
    }


    @Override
    public void setSpectator(boolean enable) {
        var cameraPacket = new PacketPlayOutCamera(enable ? this.actorMob : ((CraftPlayer)this.destinationPlayer).getHandle());
        ((CraftPlayer)this.destinationPlayer).getHandle().playerConnection.sendPacket(cameraPacket);
    }
}
