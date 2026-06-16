package commands;

import handler.KaczmarskiBot;
import handler.KaczmarskiGPTHandler;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatHandler.class);
    private static final String END_COMMAND = "/end";

    private final KaczmarskiGPTHandler gptHandler;
    private final ChatSessionManager sessionManager;
    private final MessageProvider messageProvider;

    public ChatHandler(KaczmarskiGPTHandler gptHandler, ChatSessionManager sessionManager) {
        this.gptHandler = gptHandler;
        this.sessionManager = sessionManager;
        this.messageProvider = MessageProvider.getInstance();
    }

    public void handleActiveChat(KaczmarskiBot bot, String message, long chatId) {
        logger.info(messageProvider.getMessage(MessageProvider.Keys.LOG_ACTIVE_CHAT_HANDLED,
                chatId, message));

        if (isEndCommand(message)) {
            endChat(bot, chatId);
            return;
        }

        processGptMessage(bot, message, chatId);
    }

    public void startChatWithLLM(KaczmarskiBot bot, long chatId) {
        sessionManager.startSession(chatId);
        String welcomeMessage = messageProvider.getMessage(MessageProvider.Keys.WELCOME);
        sendMessage(bot, chatId, welcomeMessage);
        logger.info(messageProvider.getMessage(MessageProvider.Keys.LOG_CHAT_STARTED, chatId));
    }

    private boolean isEndCommand(String message) {
        return END_COMMAND.equalsIgnoreCase(message.trim());
    }

    private void endChat(KaczmarskiBot bot, long chatId) {
        sessionManager.endSession(chatId);
        gptHandler.endConversation(chatId);
        String goodbyeMessage = messageProvider.getMessage(MessageProvider.Keys.GOODBYE);
        sendMessage(bot, chatId, goodbyeMessage);
        logger.info(messageProvider.getMessage(MessageProvider.Keys.LOG_CHAT_ENDED, chatId));
    }

    private void processGptMessage(KaczmarskiBot bot, String message, long chatId) {
        sendMessage(bot, chatId, "Czekaj, procesuję twoją wiadomość...");
        try {
            String response = gptHandler.processMessage(chatId, message);
            sendMessage(bot, chatId, response);
            logger.debug(messageProvider.getMessage(MessageProvider.Keys.LOG_GPT_RESPONSE,
                    chatId, response));
        } catch (Exception e) {
            logger.error("Error processing GPT message for chat {}: {}",
                    chatId, e.getMessage(), e);
            String errorMessage = messageProvider.getMessage(MessageProvider.Keys.ERROR_PROCESSING);
            sendMessage(bot, chatId, errorMessage);
        }
    }

    private void sendMessage(KaczmarskiBot bot, long chatId, String text) {
        try {
            bot.sendTextMessage(chatId, text);
        } catch (TelegramApiException e) {
            logger.error(messageProvider.getMessage(MessageProvider.Keys.ERROR_SEND_MESSAGE_FAILED,
                    chatId, e.getMessage()));
        }
    }
}
