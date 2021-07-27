package it.mattsays.cinematics.animations;

public interface Spectated {

    boolean canBeSpectated();
    void setSpectator(boolean enable);

}
