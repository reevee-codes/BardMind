package handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rag.IRagService;

import java.io.IOException;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KaczmarskiBotTests {

    @Mock
    private IRagService ragService;

    private KaczmarskiGPTHandler handler;

    @BeforeEach
    void setUp() throws IOException {
        when(ragService.buildPrompt(anyString())).thenReturn("");
        handler = new KaczmarskiGPTHandler("test-key", ragService);
    }

    @Test
    @Tag("integration")
    void testResponseTime() {
        String samplePrompt = "O czym jest utwór Mury?";

        long start = System.nanoTime();
        String response = handler.processMessage(1234L, samplePrompt);
        long end = System.nanoTime();

        long elapsedMillis = (end - start) / 1_000_000;
        System.out.println("Time that bot took to reply: " + elapsedMillis + " ms");

        assertNotNull(response);
        assertFalse(response.trim().isEmpty(), "Response can't be blank");
    }

    @Test
    @Tag("integration")
    void testConcurrentChatsDoNotMix() throws Exception {
        long chatId1 = 1001L;
        long chatId2 = 1002L;
        long chatId3 = 1003L;

        ExecutorService pool = Executors.newFixedThreadPool(3);

        Future<String> result1 = pool.submit(() -> handler.processMessage(chatId1, "O czym jest utwór Mury?"));
        Future<String> result2 = pool.submit(() -> handler.processMessage(chatId2, "O czym jest utwór Źródło?"));
        Future<String> result3 = pool.submit(() -> handler.processMessage(chatId3, "O czym jest utwór Rokosz?"));

        String response1 = result1.get(30, TimeUnit.SECONDS);
        String response2 = result2.get(30, TimeUnit.SECONDS);
        String response3 = result3.get(30, TimeUnit.SECONDS);

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);

        assertFalse(handler.chatHistoryContains(chatId2, "O czym jest utwór Mury?"),
                "Chat histories must not mix");
        assertFalse(handler.chatHistoryContains(chatId1, "O czym jest utwór Źródło?"),
                "Chat histories must not mix");

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }
}
