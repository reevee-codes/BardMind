package rag;

import com.google.gson.*;
import config.AppConfig;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EmbeddingService implements IEmbeddingService {
    private static final String OPENAI_URL = "https://api.openai.com/v1/embeddings";
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public EmbeddingService(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public List<Double> getEmbedding(String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", AppConfig.EMBEDDING_MODEL);
        body.addProperty("input", text);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API error: " + response.code() + " " + response.message());
            }

            String jsonString = response.body().string();
            JsonObject json = gson.fromJson(jsonString, JsonObject.class);
            JsonArray dataArray = json.getAsJsonArray("data");
            if (dataArray == null) {
                throw new IOException("Invalid API response: " + jsonString);
            }

            JsonArray vector = dataArray.get(0).getAsJsonObject().getAsJsonArray("embedding");
            return vector.asList().stream()
                    .map(JsonElement::getAsDouble)
                    .toList();
        }
    }
}