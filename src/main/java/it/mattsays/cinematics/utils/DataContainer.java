package it.mattsays.cinematics.utils;

public class DataContainer<T> {

    public T data;

    public DataContainer() {
        this(null);
    }

    public DataContainer(T data) {
        this.data = data;
    }
}
