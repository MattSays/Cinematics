package it.mattsays.cinematics.nms.v1_16_R3;

import it.mattsays.cinematics.nms.PathfinderGoalGoToLocation;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.Vec3D;
import org.bukkit.Location;

import java.util.EnumSet;

public class PathfinderGoalGoToLocation_1_16_R3 extends PathfinderGoal implements PathfinderGoalGoToLocation {

    private final EntityInsentient entity;
    private Vec3D destinationVector;
    private BlockPosition destinationBlock;

    private Location destinationPoint;
    private float speed;
    private boolean atDestination = false;


    public PathfinderGoalGoToLocation_1_16_R3(EntityInsentient entity) {
        this.entity = entity;
        this.a(EnumSet.of(Type.MOVE, Type.JUMP));
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public boolean isAtDestination() {
        return this.atDestination;
    }

    @Override
    public void setDestinationPoint(Location destinationPoint) {
        if (destinationPoint.equals(this.destinationPoint))
            return;

        this.destinationPoint = destinationPoint;
        this.destinationBlock = new BlockPosition(destinationPoint.getX(), destinationPoint.getY(), destinationPoint.getZ());
        this.destinationVector = new Vec3D(destinationPoint.getX(), destinationPoint.getY(), destinationPoint.getZ());
        this.atDestination = false;
    }

    @Override
    public boolean a() {
        if (destinationPoint == null) {
            return false;
        }

        if (this.entity.getWorld().getWorld().getUID() != this.destinationPoint.getWorld().getUID()) {
            return false;
        }

        if (this.entity.getPositionVector().distanceSquared(this.destinationVector) <= 0.5) {
            this.atDestination = true;
            return false;
        }

        return true;
    }

    @Override
    public void e() {
        var pathEntity = this.entity.getNavigation().a(this.destinationBlock, 0);
        this.entity.getNavigation().a(pathEntity, speed);
    }
}
