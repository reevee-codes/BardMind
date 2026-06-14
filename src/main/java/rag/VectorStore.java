package rag;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class VectorStore {
    private final List<TextChunk> chunks = new ArrayList<>();

    public void addChunk(TextChunk chunk) {
        chunks.add(chunk);
    }

    public void loadFromJson(String path) throws IOException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<TextChunk>>() {}.getType();
        List<TextChunk> loaded = gson.fromJson(Files.readString(Paths.get(path)), listType);
        chunks.clear();
        chunks.addAll(loaded);
    }

    public void saveToJson(String path) throws IOException {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(chunks, writer);
        }
    }

    public List<TextChunk> findRelevant(List<Double> queryEmbedding, int topK) {
        return chunks.stream()
                .sorted(Comparator.comparingDouble(c -> -cosineSimilarity(queryEmbedding, c.embedding())))
                .limit(topK)
                .toList();
    }

    public Set<String> getExistingSources() {
        return chunks.stream()
                .map(TextChunk::source)
                .collect(Collectors.toSet());
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dot / denominator;
    }
}
