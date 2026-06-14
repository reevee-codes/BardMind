package Commands;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSessionManager {
    private final Set<Long> activeChats;

    public ChatSessionManager() {
        this.activeChats = ConcurrentHashMap.newKeySet();
    }

    public boolean isActive(long chatId) {
        return activeChats.contains(chatId);
    }

    public void startSession(long chatId) {
        activeChats.add(chatId);
    }

    public void endSession(long chatId) {
        activeChats.remove(chatId);
    }
}

