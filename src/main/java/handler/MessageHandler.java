package handler;

import commands.CommandStorage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.*;

public class MessageHandler {
    private final CommandStorage commandStorage;

    public MessageHandler() {
        this.commandStorage = new CommandStorage();
    }

    public void handleTextMessage(KaczmarskiBot bot, Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            commandStorage.executeCommand(bot, messageText, chatId);
        } catch (Exception e) {
            bot.handleError(new TelegramApiException("Unexpected error: " + e.getMessage()));
        }
    }

    public void handlePhotoMessage(KaczmarskiBot bot, Update update) {
        long chatId = update.getMessage().getChatId();
        List<PhotoSize> photos = update.getMessage().getPhoto();

        PhotoSize largestPhoto = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null);

        if (largestPhoto == null) return;

        String caption = String.format("file_id: %s\nwidth: %d\nheight: %d",
                largestPhoto.getFileId(),
                largestPhoto.getWidth(),
                largestPhoto.getHeight());

        try {
            bot.sendPhoto(chatId, largestPhoto.getFileId(), caption);
        } catch (TelegramApiException e) {
            bot.handleError(e);
        }
    }
}
