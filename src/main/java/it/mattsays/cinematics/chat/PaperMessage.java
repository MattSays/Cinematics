package it.mattsays.cinematics.chat;

import de.themoep.minedown.MineDown;
import it.mattsays.cinematics.commons.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

public class PaperMessage extends Message {

    public PaperMessage(String message) {
        super(message);
    }

    public PaperMessage(String configPath, Object config) {
        super(configPath, config);
    }

    @Override
    public Message resolvePlaceholder(String placeHolder, String replacement) {
        return new PaperMessage(this.message.replace(placeHolder, replacement));
    }

    @Override
    public void loadFromConfig(Object config) {
        this.message = ((Configuration)config).getString(configPath, CANNOT_READ_DATA + configPath);
    }

    @Override
    public Message send(Object receiver) {
        ((CommandSender)receiver).sendMessage(MineDown.parse(this.getMessageData()));
        return this;
    }
}
