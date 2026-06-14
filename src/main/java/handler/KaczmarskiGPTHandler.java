package handler;

import config.AppConfig;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import rag.IRagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KaczmarskiGPTHandler {

    private static final Logger logger = LoggerFactory.getLogger(KaczmarskiGPTHandler.class);

    private final String systemPromptTemplate;
    private final OpenAiService openAiService;
    private final Map<Long, List<ChatMessage>> chatHistories;
    private final Properties lyrics;
    private final IRagService ragService;

    public KaczmarskiGPTHandler(String apiKey, IRagService ragService) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (ragService == null) {
            throw new IllegalArgumentException("RagService cannot be null");
        }
        
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(AppConfig.OPENAI_TIMEOUT_SECONDS));
        this.chatHistories = new ConcurrentHashMap<>();
        this.lyrics = loadLyrics();
        this.systemPromptTemplate = loadSystemPromptTemplate();
        this.ragService = ragService;
    }

    private String loadSystemPromptTemplate() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("prompts/system_prompt.txt")) {
            if (input == null) {
                throw new IOException("prompts/system_prompt.txt not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to load system prompt template", e);
            return "Jesteś Jackiem Kaczmarskim.\n%s";
        }
    }

    private Properties loadLyrics() {
        Properties lyrics = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("lyrics.properties")) {
            if (input == null) {
                throw new IOException("Lyrics file not found");
            }
            lyrics.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("Failed to load lyrics, will proceed without them", e);
        }
        return lyrics;
    }

    private String formatLyricsForPrompt() {
        StringBuilder sb = new StringBuilder();
        for (String key : lyrics.stringPropertyNames()) {
            String[] parts = lyrics.getProperty(key).split("\\|");
            if (parts.length == 2) {
                sb.append("\"").append(parts[0]).append("\": ").append(parts[1].replace("\\n", "\n")).append("\n");
            }
        }
        return sb.toString();
    }

    public String processMessage(long chatId, String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        String context = "";
        try {
            context = ragService.buildPrompt(message);
        } catch (IOException e) {
            logger.warn("Nie udało się pobrać kontekstu z RAG: {}", e.getMessage(), e);
        }
        try {
            String textRequest = handleTextRequest(message);
            if (textRequest != null) {
                return textRequest;
            }

            List<ChatMessage> messages = chatHistories.computeIfAbsent(chatId, k -> Collections.synchronizedList(new ArrayList<>()))
            ;

            if (messages.isEmpty()) {
                String formattedPrompt = String.format(systemPromptTemplate, formatLyricsForPrompt());
                messages.add(new ChatMessage("system", formattedPrompt));
            }

            String userContent = context.isEmpty() ? message : context + "\n\n" + message;
            messages.add(new ChatMessage("user", userContent));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .messages(messages)
                    .model(AppConfig.PRIMARY_MODEL)
                    .temperature(AppConfig.TEMPERATURE)
                    .maxTokens(AppConfig.MAX_TOKENS)
                    .presencePenalty(AppConfig.PRESENCE_PENALTY)
                    .frequencyPenalty(AppConfig.FREQUENCY_PENALTY)
                    .build();

            try {
                ChatMessage response = openAiService.createChatCompletion(completionRequest)
                        .getChoices().get(0).getMessage();
                messages.add(response);
                return response.getContent();
            } catch (Exception e) {
                logger.warn("Falling back to GPT-3.5-turbo due to: {}", e.getMessage(), e);

                ChatCompletionRequest fallbackRequest = ChatCompletionRequest.builder()
                        .messages(messages)
                        .model(AppConfig.FALLBACK_MODEL)
                        .temperature(AppConfig.TEMPERATURE)
                        .maxTokens(AppConfig.MAX_TOKENS)
                        .presencePenalty(AppConfig.PRESENCE_PENALTY)
                        .frequencyPenalty(AppConfig.FREQUENCY_PENALTY)
                        .build();

                ChatMessage response = openAiService.createChatCompletion(fallbackRequest)
                        .getChoices().get(0).getMessage();
                messages.add(response);
                return response.getContent();
            }

        } catch (Exception e) {
            logger.error("Error processing message for chatId {}: {}", chatId, e.getMessage(), e);
            return "Przepraszam, coś poszło nie tak. Może rozpocznijmy rozmowę od nowa?";
        }
    }

    public void endConversation(long chatId) {
        if (chatHistories.containsKey(chatId)) {
            chatHistories.remove(chatId);
            logger.debug("Ended conversation for chatId: {}", chatId);
        }
    }

    public boolean chatHistoryContains(long chatId, String text) {
        List<ChatMessage> messages = chatHistories.get(chatId);
        if (messages == null) {
            return false;
        }
        return messages.stream()
                .anyMatch(msg -> msg.getContent() != null && msg.getContent().contains(text));
    }

    public int getChatHistorySize(long chatId) {
        List<ChatMessage> messages = chatHistories.get(chatId);
        return messages != null ? messages.size() : 0;
    }

    public String handleTextRequest(String message) {
        if (message == null) {
            return null;
        }
        String lowerMessage = message.toLowerCase();

        if (!lowerMessage.contains("tekst") && !lowerMessage.contains("daj")) {
            return null;
        }

        for (String key : lyrics.stringPropertyNames()) {
            String[] parts = lyrics.getProperty(key).split("\\|");
            if (parts.length == 2) {
                String title = parts[0].toLowerCase();
                String text = parts[1].replace("\\n", "\n");
                if (lowerMessage.contains(title) ||
                        lowerMessage.contains(getBaseForm(title)) ||
                        title.contains(getBaseForm(lowerMessage))) {
                    return "\"" + parts[0] + "\"\n\n" + text;
                }
            }
        }

        StringBuilder availableSongs = new StringBuilder();
        availableSongs.append("Dostępne utwory:\n");
        for (String key : lyrics.stringPropertyNames()) {
            String[] parts = lyrics.getProperty(key).split("\\|");
            if (parts.length == 2) {
                availableSongs.append("- ").append(parts[0]).append("\n");
            }
        }
        availableSongs.append("\nNapisz np. 'daj tekst Murów' aby otrzymać konkretny tekst.");

        return availableSongs.toString();
    }

    private String getBaseForm(String word) {
        String base = word.toLowerCase();

        if (base.endsWith("ów")) {
            return base.substring(0, base.length() - 2);
        }
        if (base.endsWith("y") || base.endsWith("i")) {
            return base.substring(0, base.length() - 1);
        }
        if (base.endsWith("a") || base.endsWith("ę")) {
            return base.substring(0, base.length() - 1);
        }

        return base;
    }
} 