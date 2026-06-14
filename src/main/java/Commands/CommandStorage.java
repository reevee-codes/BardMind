package commands;

import config.AppConfig;
import exceptions.ConfigurationException;
import exceptions.RagException;
import handler.KaczmarskiBot;
import handler.KaczmarskiGPTHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rag.EmbeddingService;
import rag.RagService;
import utils.PropertiesLoader;

import java.io.IOException;
import java.util.Properties;

public class CommandStorage {
    private static final Logger logger = LoggerFactory.getLogger(CommandStorage.class);
    private static final String START_COMMAND = "/start";
    private static final String UNKNOWN_COMMAND_MESSAGE =
            "Nieznana komenda. Użyj /help aby zobaczyć dostępne komendy lub /start aby rozpocząć " +
                    "rozmowę.";

    private final ChatSessionManager sessionManager;
    private final CommandExecutor commandExecutor;
    private final ChatHandler chatHandler;
    private final KaczmarskiGPTHandler gptHandler;
    private final EmbeddingService embeddingService;
    private final RagService ragService;

    public CommandStorage() {
        Properties commands = PropertiesLoader.load("commands.properties");
        String apiKey = loadApiKey();

        this.sessionManager = new ChatSessionManager();
        this.commandExecutor = new CommandExecutor(commands);
        this.embeddingService = new EmbeddingService(apiKey);
        this.ragService = new RagService(embeddingService);
        try {
            this.ragService.getStore().loadFromJson(AppConfig.EMBEDDINGS_JSON_PATH);
        } catch (IOException e) {
            throw new RagException("Failed to load embeddings from JSON file", e);
        }
        this.gptHandler = new KaczmarskiGPTHandler(apiKey, ragService);
        this.chatHandler = new ChatHandler(gptHandler, sessionManager);
    }

    /** Routes an incoming message: lyrics lookup → active session → /start → static commands → unknown. */
    public void executeCommand(KaczmarskiBot bot, String message, long chatId) {
        if (bot == null) {
            throw new IllegalArgumentException("Bot cannot be null");
        }
        if (message == null || message.trim().isEmpty()) {
            logger.warn("Received empty message from chat: {}", chatId);
            return;
        }
        
        String trimmedMessage = message.trim();
        logger.info("Received message: {} from chat: {}", trimmedMessage, chatId);

        if (sessionManager.isActive(chatId)) {
            chatHandler.handleActiveChat(bot, trimmedMessage, chatId);
            return;
        }

        String textRequest = handleTextRequest(trimmedMessage);
        if (textRequest != null) {
            try {
                bot.sendTextMessage(chatId, textRequest);
                return;
            } catch (Exception e) {
                logger.error("Failed to send text request to chat {}: {}", chatId, e.getMessage(), e);
            }
        }
        if (isStartCommand(trimmedMessage)) {
            chatHandler.startChatWithLLM(bot, chatId);
            return;
        }
        if (!commandExecutor.executeIfExists(bot, trimmedMessage, chatId)) {
            sendUnknownCommandMessage(bot, chatId);
        }
    }

    private boolean isStartCommand(String message) {
        return START_COMMAND.equalsIgnoreCase(message);
    }

    private void sendUnknownCommandMessage(KaczmarskiBot bot, long chatId) {
        try {
            bot.sendTextMessage(chatId, UNKNOWN_COMMAND_MESSAGE);
        } catch (Exception e) {
            logger.error("Failed to send unknown command message to chat {}: {}",
                    chatId, e.getMessage(), e);
        }
    }

    private String loadApiKey() {
        Properties config = PropertiesLoader.load("config.properties");
        String apiKey = config.getProperty("openai.api.key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ConfigurationException("OpenAI API key not found in config.properties");
        }
        return apiKey;
    }
    
    private String handleTextRequest(String message) {
        return gptHandler.handleTextRequest(message);
    }
}
