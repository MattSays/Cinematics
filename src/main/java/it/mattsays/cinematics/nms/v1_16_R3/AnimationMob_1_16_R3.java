package it.mattsays.cinematics.nms.v1_16_R3;

import it.mattsays.cinematics.Cinematics;
import it.mattsays.cinematics.animations.AnimationMob;
import it.mattsays.cinematics.utils.DataContainer;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class AnimationMob_1_16_R3 extends AnimationMob {

    private MoveableMob_1_16_R3 actorMob;


    public AnimationMob_1_16_R3(UUID id, Player player) {
        super(id, player);
    }

    @Override
    public Location getCurrentLocation() {
        return this.actorMob.getBukkitEntity().getLocation();
    }

    @Override
    public boolean isArrivedAtDestination() {
        return this.actorMob.getPathfinderGoal().isAtDestination();
    }

    private Optional<MoveableMob_1_16_R3> getNMSCreature(EntityType mobType, Location spawnLocation, float speed) {
        var mobName = mobType.getEntityClass().getSimpleName();

        try {
            var entityType = (EntityTypes<? extends EntityInsentient>) EntityTypes.class.getField(mobName.toUpperCase()).get(null);
            var mob = new MoveableMob_1_16_R3(entityType, spawnLocation, speed);
            return Optional.of(mob);
        } catch (Exception e) {
            Cinematics.LOGGER.error("Bukkit to NMS entity conversion error > " + e.getMessage());
            return Optional.empty();
        }


    }


    @Override
    public void spawn(Location location) {

        var mobContainer = new DataContainer<MoveableMob_1_16_R3>();

        this.getNMSCreature(this.mobType, location, 1.25f).ifPresent(mob -> {
            mobContainer.data = mob;
        });

        this.actorMob = mobContainer.data;

        this.actorMob.getPathfinderGoal().setSpeed(this.speed);
        this.actorMob.getPathfinderGoal().setDestinationPoint(this.currentDestinationPoint);

        this.actorMob.setInvulnerable(true);

        var spawnPacket = new PacketPlayOutSpawnEntityLiving(this.actorMob);
        var metadataPacket = new PacketPlayOutEntityMetadata(this.actorMob.getId(), this.actorMob.getDataWatcher(), false);
        var headRotationPacket = new PacketPlayOutEntityHeadRotation(this.actorMob, (byte) ((location.getYaw() / 360.0d) * 255.0d));

        var entityPlayer = ((CraftPlayer) this.destinationPlayer).getHandle();

        entityPlayer.playerConnection.sendPacket(spawnPacket);
        entityPlayer.playerConnection.sendPacket(metadataPacket);
        entityPlayer.playerConnection.sendPacket(headRotationPacket);
    }

    @Override
    public void teleportTo(Location location) {
        this.actorMob.setLocation(
                location.getX(), location.getY(), location.getZ(),
                0, location.getPitch()
        );
    }

    @Override
    public void update() {

        var teleportPacket = new PacketPlayOutEntityTeleport(this.actorMob);
        var headRotationPacket = new PacketPlayOutEntityHeadRotation(this.actorMob, (byte) ((this.actorMob.getHeadRotation() / 360.0d) * 255.0d));

        var entityPlayer = ((CraftPlayer) this.destinationPlayer).getHandle();
        entityPlayer.playerConnection.sendPacket(headRotationPacket);
        entityPlayer.playerConnection.sendPacket(teleportPacket);
    }

    @Override
    public void tick() {
        if(this.actorMob != null)
            this.actorMob.tick();
    }

    @Override
    public void setDestinationPoint(Location location) {
        this.actorMob.getPathfinderGoal().setDestinationPoint(location);
    }

    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);
        this.actorMob.getPathfinderGoal().setSpeed(speed);
    }

    @Override
    public void setSpectator(boolean enable) {
        var cameraPacket = new PacketPlayOutCamera(enable ? this.actorMob : ((CraftPlayer) this.destinationPlayer).getHandle());
        ((CraftPlayer) this.destinationPlayer).getHandle().playerConnection.sendPacket(cameraPacket);
    }

    @Override
    public void destroy() {
        var killPacket = new PacketPlayOutEntityDestroy(this.actorMob.getId());
        ((CraftPlayer) this.destinationPlayer).getHandle().playerConnection.sendPacket(killPacket);
    }


}
