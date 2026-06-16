package rag;

import java.io.IOException;

/**
 * Fetches plain-text article content from Wikipedia.
 * Abstracted as an interface so it can be mocked in tests.
 */
public interface WikipediaClient {

    /**
     * Fetches the plain-text extract of a Wikipedia article.
     *
     * @param title    article title as it appears in the Wikipedia URL (e.g. "Jacek_Kaczmarski")
     * @param language ISO 639-1 language code (e.g. "pl", "en")
     * @return full plain-text content, or an empty string if the article does not exist
     * @throws IOException on network or API errors
     */
    String fetchContent(String title, String language) throws IOException;
}
