package it.mattsays.cinematics.animations;

public interface Spectated {

    public boolean canBeSpectated();

    public void setSpectator(boolean enable);

}
