package commands;

import handler.KaczmarskiBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.Properties;

public class CommandExecutor {
    private final Properties commands;

    public CommandExecutor(Properties commands) {
        this.commands = commands;
    }

    public boolean executeIfExists(KaczmarskiBot bot, String message, long chatId) {
        String action = commands.getProperty(message);
        if (action == null) {
            return false;
        }

        executeAction(bot, chatId, action);
        return true;
    }

    private void executeAction(KaczmarskiBot bot, long chatId, String action) {
        String[] actions = action.split(";");
        for (String singleAction : actions) {
            executeSingleAction(bot, chatId, singleAction);
        }
    }

    private void executeSingleAction(KaczmarskiBot bot, long chatId, String action) {
        String[] parts = action.split(":", 2);
        String type = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        try {
            switch (type) {
                case "TEXT" -> bot.sendTextMessage(chatId, data);
                case "PHOTO" -> bot.sendPhoto(chatId, data, "");
                case "MUSIC" -> bot.sendMusic(chatId, data);
                default -> bot.sendTextMessage(chatId, "Unknown command type: " + type);
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to execute action: " + action, e);
        }
    }
}

