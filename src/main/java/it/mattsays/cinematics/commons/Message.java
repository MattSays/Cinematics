package it.mattsays.cinematics.commons;

public abstract class Message {

    protected final static String CANNOT_READ_DATA = "Cannot read message from ";
    protected String message;
    protected String configPath;

    public Message(String message) {
        this.message = message;
    }

    public Message(String configPath, Object config) {
        this.configPath = configPath;
        this.loadFromConfig(config);
    }

    public String getMessageData() {
        return message;
    }

    public abstract Message resolvePlaceholder(String placeHolder, String replacement);

    public abstract void loadFromConfig(Object config);

    public abstract Message send(Object receiver);

}

