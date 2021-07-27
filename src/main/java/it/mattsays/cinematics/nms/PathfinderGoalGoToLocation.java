package it.mattsays.cinematics.nms;

import org.bukkit.Location;

public interface PathfinderGoalGoToLocation {

    void setSpeed(float speed);

    boolean isAtDestination();

    void setDestinationPoint(Location destinationPoint);


}
