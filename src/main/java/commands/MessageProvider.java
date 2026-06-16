package commands;

import utils.PropertiesLoader;
import java.text.MessageFormat;
import java.util.Properties;

public class MessageProvider {
    private static final MessageProvider INSTANCE = new MessageProvider();
    private final Properties messages;

    private MessageProvider() {
        this.messages = PropertiesLoader.load("messages.properties");
    }

    public static MessageProvider getInstance() {
        return INSTANCE;
    }

    public String getMessage(String key) {
        String message = messages.getProperty(key);
        if (message == null) {
            throw new IllegalArgumentException("Message not found for key: " + key);
        }
        return message;
    }

    public String getMessage(String key, Object... args) {
        String template = getMessage(key);
        return MessageFormat.format(template, args);
    }

    public static class Keys {
        public static final String WELCOME = "chat.welcome";
        public static final String GOODBYE = "chat.goodbye";
        public static final String ERROR_PROCESSING = "chat.error.processing";
        public static final String ERROR_SEND_MESSAGE_FAILED = "error.send.message.failed";
        public static final String LOG_CHAT_STARTED = "log.chat.started";
        public static final String LOG_CHAT_ENDED = "log.chat.ended";
        public static final String LOG_GPT_RESPONSE = "log.gpt.response";
        public static final String LOG_ACTIVE_CHAT_HANDLED = "log.active.chat.handled";
    }
}
