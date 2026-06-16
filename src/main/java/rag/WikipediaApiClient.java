package rag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Wikipedia MediaWiki Action API client.
 * Uses the {@code action=query&prop=extracts&explaintext=true} endpoint
 * to retrieve plain-text article content without markup.
 *
 * <p>Wikipedia's API requires a descriptive User-Agent.
 * See <a href="https://www.mediawiki.org/wiki/API:Etiquette">API Etiquette</a>.
 */
public final class WikipediaApiClient implements WikipediaClient {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaApiClient.class);

    private static final String API_HOST_TEMPLATE = "%s.wikipedia.org";
    private static final String API_PATH = "/w/api.php";
    private static final String USER_AGENT = "BardMind/1.0 (educational Kaczmarski bot; https://github.com/bardmind)";

    private static final Map<String, String> BASE_PARAMS = Map.of(
            "action", "query",
            "prop", "extracts",
            "explaintext", "true",
            "redirects", "1",
            "format", "json",
            "utf8", "1"
    );

    private final OkHttpClient httpClient;

    public WikipediaApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String fetchContent(String title, String language) throws IOException {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(language, "language must not be null");

        HttpUrl url = buildUrl(title, language);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();

        logger.debug("Fetching Wikipedia article: {} ({})", title, language);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Wikipedia API returned HTTP " + response.code() + " for: " + title);
            }
            try (ResponseBody body = response.body()) {
                if (body == null) throw new IOException("Empty response body for: " + title);
                return parseExtract(body.string(), title);
            }
        }
    }

    private HttpUrl buildUrl(String title, String language) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme("https")
                .host(String.format(API_HOST_TEMPLATE, language))
                .addPathSegments(API_PATH.substring(1)) // strip leading slash
                .addQueryParameter("titles", title);

        BASE_PARAMS.forEach(builder::addQueryParameter);
        return builder.build();
    }

    private String parseExtract(String json, String title) {
        JsonObject pages = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject("query")
                .getAsJsonObject("pages");

        // Pages is keyed by page ID; there is exactly one entry for a single-title query
        JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();

        if (page.has("missing")) {
            logger.warn("Article not found on Wikipedia: {}", title);
            return "";
        }

        return page.has("extract") ? page.get("extract").getAsString() : "";
    }
}
