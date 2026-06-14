package rag;

import config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

public class RagService implements IRagService {
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    private final IEmbeddingService embeddingService;
    private final VectorStore store = new VectorStore();

    public RagService(IEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public void loadFiles(String folderPath) throws IOException {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder path cannot be null or empty");
        }
        
        Path embeddingsPath = Paths.get(AppConfig.EMBEDDINGS_JSON_PATH);
        if (Files.exists(embeddingsPath)) {
            store.loadFromJson(embeddingsPath.toString());
        }

        Set<String> existing = store.getExistingSources();
        Files.list(Paths.get(folderPath))
                .filter(p -> p.toString().endsWith(".txt"))
                .filter(p -> !existing.contains(p.getFileName().toString()))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        var getEmbeddingOfContent = embeddingService.getEmbedding(content);
                        store.addChunk(new TextChunk(path.getFileName().toString(), content, getEmbeddingOfContent));
                        logger.info("Embedded: {}", path.getFileName());
                    } catch (Exception e) {
                        logger.error("Error processing {}: {}", path.getFileName(), e.getMessage(), e);
                    }
                });

        store.saveToJson(embeddingsPath.toString());
    }

    /** Returns a RAG context string to prepend to the user message, or empty if no relevant chunks found. */
    public String buildPrompt(String userQuestion) throws IOException {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            throw new IllegalArgumentException("User question cannot be null or empty");
        }
        
        var queryEmbedding = embeddingService.getEmbedding(userQuestion);
        var top = store.findRelevant(queryEmbedding, AppConfig.TOP_K_RESULTS);
        StringBuilder sb = new StringBuilder("Jesteś Jackiem Kaczmarskim.\n");
        sb.append("Na podstawie poniższych fragmentów odpowiadasz poetycko:\n\n");
        for (var chunk : top) {
            sb.append("[").append(chunk.source()).append("]\n");
            sb.append(chunk.text()).append("\n\n");
        }
        sb.append("Pytanie: ").append(userQuestion).append("\nOdpowiedź:");
        return sb.toString();
    }

    public VectorStore getStore() {
        return store;
    }
}