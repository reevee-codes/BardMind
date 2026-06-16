package rag.tools;

import config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rag.TextChunker;
import rag.WikipediaApiClient;
import rag.WikipediaClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * One-shot CLI tool that downloads Wikipedia articles about Jacek Kaczmarski,
 * chunks them, and writes each chunk as a {@code .txt} file into the RAG data folder.
 *
 * <p>After running this, execute {@code mvn exec:java@generate-embeddings} to embed the new files.
 *
 * <p>The tool is <b>idempotent</b>: files that already exist are skipped,
 * so it is safe to run multiple times or after adding new articles.
 */
public final class WikipediaImporter {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaImporter.class);

    /** Target chunk size in characters (~400–600 tokens for text-embedding-3-small). */
    private static final int CHUNK_SIZE = 2000;

    /** Overlap carried over as context seed to the next chunk. */
    private static final int OVERLAP = 200;

    /** Polite delay between Wikipedia API requests (ms). */
    private static final long REQUEST_DELAY_MS = 500;

    /**
     * Articles to import. Add entries here to enrich the bot's knowledge.
     *
     * @param language ISO 639-1 code ("pl", "en")
     * @param title    Wikipedia article title (URL form, e.g. "Jacek_Kaczmarski")
     */
    private record ArticleRef(String language, String title) {
        String filePrefix() {
            return String.format("wiki_%s_%s", language, title.replace("(", "").replace(")", "").replace(" ", "_"));
        }
    }

    private static final List<ArticleRef> ARTICLES = List.of(
            new ArticleRef("pl", "Jacek_Kaczmarski"),
            new ArticleRef("pl", "Mury_(piosenka)"),
            new ArticleRef("pl", "Zbigniew_Łapiński_(muzyk)"),
            new ArticleRef("en", "Jacek_Kaczmarski")
    );

    private final WikipediaClient client;
    private final TextChunker chunker;
    private final Path dataFolder;

    public WikipediaImporter(WikipediaClient client, TextChunker chunker, Path dataFolder) {
        this.client = client;
        this.chunker = chunker;
        this.dataFolder = dataFolder;
    }

    /**
     * Imports all configured articles. Errors on individual articles are logged
     * and do not abort the remaining imports.
     *
     * @return total number of chunk files written
     */
    public int run() throws IOException {
        Files.createDirectories(dataFolder);
        int totalWritten = 0;

        for (ArticleRef article : ARTICLES) {
            logger.info("→ Fetching [{}/{}] {}...", article.language(), article.title(), "");
            try {
                totalWritten += importArticle(article);
                Thread.sleep(REQUEST_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Import interrupted — stopping early.");
                break;
            } catch (IOException e) {
                logger.error("  ✗ Failed to fetch '{}': {}", article.title(), e.getMessage());
            }
        }

        return totalWritten;
    }

    private int importArticle(ArticleRef article) throws IOException {
        String content = client.fetchContent(article.title(), article.language());
        if (content.isBlank()) {
            logger.warn("  ✗ Article empty or not found: {}", article.title());
            return 0;
        }

        List<String> chunks = chunker.chunk(content);
        int written = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String filename = String.format("%s_%02d.txt", article.filePrefix(), i + 1);
            Path target = dataFolder.resolve(filename);

            if (Files.exists(target)) {
                logger.debug("  ↷ Skipping existing: {}", filename);
                continue;
            }

            Files.writeString(target, chunks.get(i), StandardCharsets.UTF_8);
            written++;
            logger.debug("  ✓ Wrote {}", filename);
        }

        logger.info("  ✓ '{}': {}/{} chunks written ({}  skipped)",
                article.title(), written, chunks.size(), chunks.size() - written);
        return written;
    }

    public static void main(String[] args) throws IOException {
        WikipediaClient client = new WikipediaApiClient();
        TextChunker chunker = new TextChunker(CHUNK_SIZE, OVERLAP);
        Path dataFolder = Paths.get(AppConfig.DATA_FOLDER_PATH);

        WikipediaImporter importer = new WikipediaImporter(client, chunker, dataFolder);
        int written = importer.run();

        if (written > 0) {
            logger.info("✅ Done. {} chunk(s) written → run 'mvn exec:java@generate-embeddings' to embed them.", written);
        } else {
            logger.info("✅ Done. No new chunks written (all files already exist).");
        }
    }
}
