package rag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits long text into overlapping chunks suitable for embedding.
 * Uses paragraph-aware splitting to avoid cutting mid-sentence.
 * The last paragraph of each chunk is reused as the first paragraph of the next
 * to preserve semantic continuity across chunk boundaries.
 */
public final class TextChunker {

    private final int chunkSize;
    private final int overlap;

    /**
     * @param chunkSize target character count per chunk (soft limit — never cuts mid-paragraph)
     * @param overlap   max character count kept from the previous chunk as context seed
     */
    public TextChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be > 0");
        if (overlap < 0 || overlap >= chunkSize) throw new IllegalArgumentException("overlap must be in [0, chunkSize)");
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    /**
     * Splits the given text into overlapping chunks.
     *
     * @param text source text (may contain multiple paragraphs separated by blank lines)
     * @return ordered list of chunk strings; empty list if text is blank
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        List<String> paragraphs = splitParagraphs(text);
        if (paragraphs.isEmpty()) return Collections.emptyList();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String overlapSeed = "";

        for (String para : paragraphs) {
            boolean wouldExceed = current.length() + para.length() > chunkSize;

            if (wouldExceed && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
                if (!overlapSeed.isEmpty()) {
                    current.append(overlapSeed).append("\n\n");
                }
            }

            current.append(para).append("\n\n");
            // Track the last paragraph as the overlap seed for the next chunk
            if (para.length() <= overlap) {
                overlapSeed = para;
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return Collections.unmodifiableList(chunks);
    }

    private List<String> splitParagraphs(String text) {
        List<String> result = new ArrayList<>();
        for (String para : text.split("\n{2,}")) {
            String trimmed = para.trim();
            // Skip very short lines (section headers, noise)
            if (trimmed.length() >= 20) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
