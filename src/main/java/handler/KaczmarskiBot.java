package handler;

import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import utils.PropertiesLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class KaczmarskiBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(KaczmarskiBot.class);

    private final MessageHandler messageHandler = new MessageHandler();
    private static final Properties configProperties = PropertiesLoader.load("config.properties");
    private static final String BOT_TOKEN = configProperties.getProperty("telegram.bot.token");
    private static final String BOT_USERNAME = configProperties.getProperty("bot.username");

    public KaczmarskiBot() {
        super(BOT_TOKEN);
        clearAllWebhooks();
        }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        if (update.getMessage().hasText()) {
            messageHandler.handleTextMessage(this, update);
        } else if (update.getMessage().hasPhoto()) {
            messageHandler.handlePhotoMessage(this, update);
        }
    }

    public void sendMusic(Long chatId, String filePath) {
        SendAudio sendAudio = new SendAudio();
        sendAudio.setChatId(chatId);
        filePath = filePath.replace("\"", "").trim();
        sendAudio.setAudio(new InputFile(new File(filePath)));
        try {
            execute(sendAudio);
        } catch (TelegramApiException e) {
            logger.warn("Failed to send audio to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void sendTextMessage(long chatId, String text) throws TelegramApiException {
        execute(createMessage(chatId, text));
    }

    public void sendPhoto(long chatId, String photoSource, String caption) throws TelegramApiException {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setPhoto(new InputFile(photoSource));
        photo.setCaption(caption);
        execute(photo);
    }

    private SendMessage createMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }

    void handleError(TelegramApiException e) {
        logger.error("Telegram API error: {}", e.getMessage(), e);
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public void clearAllWebhooks() {
        try {
            DeleteWebhook deleteWebhook = new DeleteWebhook();
            deleteWebhook.setDropPendingUpdates(true);

            execute(deleteWebhook);
            logger.info("Bot secured - webhooks cleared");
        } catch (TelegramApiException e) {
            logger.warn("Webhook cleanup failed: {}", e.getMessage(), e);
        }
    }
}

                