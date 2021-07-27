package it.mattsays.cinematics.nms.v1_16_R3;

import it.mattsays.cinematics.nms.MoveableMob;
import it.mattsays.cinematics.nms.PathfinderGoalGoToLocation;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.PathfinderGoalFloat;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

public class MoveableMob_1_16_R3 extends EntityInsentient implements MoveableMob {

    private PathfinderGoalGoToLocation_1_16_R3 goToLocation;

    public MoveableMob_1_16_R3(EntityTypes<? extends EntityInsentient> type, Location spawnLocation, float speed) {
        super(type, ((CraftWorld) spawnLocation.getWorld()).getHandle());
        this.setPositionRotation(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());
        this.getPathfinderGoal().setSpeed(speed);
    }

    @Override
    protected void initPathfinder() {
        this.goToLocation = new PathfinderGoalGoToLocation_1_16_R3(this);
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(1, this.goToLocation);
    }


    @Override
    public PathfinderGoalGoToLocation getPathfinderGoal() {
        return goToLocation;
    }
}
