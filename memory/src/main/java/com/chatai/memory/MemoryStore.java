package com.chatai.memory;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.chatai.aiinteract.models.AiMessage;
import com.chatai.memory.storage.MemoryDatabase;
import com.chatai.memory.storage.MemoryEntryEntity;
import com.chatai.memory.storage.StoredMessageEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent storage backend powered by Room (SQLite + FTS4).
 *
 * <p>Two-tier storage:
 * <ol>
 *   <li><b>Messages</b> — raw conversation history with FTS4 full-text search.
 *       Provides the short-term context window for LLM calls.</li>
 *   <li><b>Memory Entries</b> — durable facts extracted from conversations.
 *       Provides long-term memory that survives indefinitely.</li>
 * </ol>
 *
 * <p>All write operations are dispatched to a background thread.
 * Reads happen on the calling thread (Room queries are fast for SQLite).
 */
class MemoryStore {

    private final MemoryDatabase db;
    private final HandlerThread handlerThread;
    private final Handler storeHandler;

    MemoryStore(Context context) {
        this.db = MemoryDatabase.getInstance(context);

        this.handlerThread = new HandlerThread("MemoryStore");
        this.handlerThread.start();
        this.storeHandler = new Handler(handlerThread.getLooper());
    }

    // ==================== Messages (Short-term Context) ====================

    /**
     * Store a UI message in the database.
     */
    void addMessage(String senderId, String text, int type, String mediaPath, long duration) {
        StoredMessageEntity entity = new StoredMessageEntity(
            senderId, text, type, mediaPath, null, duration, System.currentTimeMillis()
        );
        runInBackground(() -> db.messageDao().insert(entity));
    }

    /**
     * Store an AiMessage in the database.
     */
    void addAiMessage(AiMessage aiMessage) {
        String senderId = aiMessage.getRole() == AiMessage.Role.USER ? "user" : "ai";
        int type = aiMessageTypeToInt(aiMessage.getMessageType());

        StoredMessageEntity entity = new StoredMessageEntity(
            senderId,
            aiMessage.getContent(),
            type,
            aiMessage.getLocalMediaPath(),
            aiMessage.getMediaUrl(),
            aiMessage.getDuration(),
            System.currentTimeMillis()
        );
        runInBackground(() -> db.messageDao().insert(entity));
    }

    /**
     * Get the most recent N messages for LLM context.
     */
    List<AiMessage> getRecentContext(int maxMessages) {
        List<StoredMessageEntity> entities = db.messageDao().getRecentMessages(maxMessages);
        List<AiMessage> result = new ArrayList<>();
        for (StoredMessageEntity e : entities) {
            result.add(e.toAiMessage());
        }
        return result;
    }

    /**
     * Search messages by keyword via FTS4.
     */
    List<AiMessage> searchMessages(String query, int maxResults) {
        String ftsQuery = query.trim().replaceAll("[^\\w\\s]", "") + "*";
        List<StoredMessageEntity> entities = db.messageDao().searchByKeyword(ftsQuery, maxResults);
        List<AiMessage> result = new ArrayList<>();
        for (StoredMessageEntity e : entities) {
            result.add(e.toAiMessage());
        }
        return result;
    }

    /**
     * Search messages excluding recent ones (to avoid duplicate context).
     */
    List<AiMessage> searchExcludingRecent(String query, int excludeCount, int maxResults) {
        String ftsQuery = query.trim().replaceAll("[^\\w\\s]", "") + "*";
        List<StoredMessageEntity> entities = db.messageDao()
            .searchExcludingRecent(ftsQuery, excludeCount, maxResults);
        List<AiMessage> result = new ArrayList<>();
        for (StoredMessageEntity e : entities) {
            result.add(e.toAiMessage());
        }
        return result;
    }

    int getMessageCount() {
        return db.messageDao().getCount();
    }

    void clearMessages() {
        runInBackground(() -> db.messageDao().deleteAll());
    }

    // ==================== Memory Entries (Long-term Memory) ====================

    /**
     * Store a long-term memory fact.
     *
     * @param key        Unique key (e.g., "user_name")
     * @param value      The fact content
     * @param summary    Short description for display
     * @param importance 0-10 score
     */
    void putMemory(String key, String value, String summary, int importance) {
        runInBackground(() -> {
            MemoryEntryEntity existing = db.memoryEntryDao().getByKey(key);
            MemoryEntryEntity entry;
            if (existing != null) {
                entry = existing;
                entry.setMemoryValue(value);
                entry.setSummary(summary);
                entry.setImportance(importance);
                entry.setUpdatedAt(System.currentTimeMillis());
            } else {
                entry = new MemoryEntryEntity(key, value, summary, importance);
            }
            db.memoryEntryDao().upsert(entry);
        });
    }

    /**
     * Get a specific memory by key.
     */
    MemoryEntryEntity getMemory(String key) {
        MemoryEntryEntity entry = db.memoryEntryDao().getByKey(key);
        if (entry != null) {
            recordMemoryAccess(entry.getId());
        }
        return entry;
    }

    /**
     * Search memory entries by keyword.
     */
    List<MemoryEntryEntity> searchMemories(String query, int limit) {
        List<MemoryEntryEntity> results = db.memoryEntryDao().search(query, limit);
        for (MemoryEntryEntity e : results) {
            recordMemoryAccess(e.getId());
        }
        return results;
    }

    /**
     * Get top memories sorted by importance and recency.
     */
    List<MemoryEntryEntity> getTopMemories(int limit) {
        return db.memoryEntryDao().getTopMemories(limit);
    }

    /**
     * Build a system prompt string from the top long-term memories.
     */
    String buildMemoryContext(int maxMemories) {
        List<MemoryEntryEntity> topMemories = getTopMemories(maxMemories);
        if (topMemories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[Long-term memories about the user]\n");
        for (MemoryEntryEntity m : topMemories) {
            sb.append("- ").append(m.getMemoryValue()).append("\n");
        }
        return sb.toString();
    }

    int getMemoryCount() {
        return db.memoryEntryDao().getCount();
    }

    void deleteMemory(String key) {
        runInBackground(() -> db.memoryEntryDao().deleteByKey(key));
    }

    void clearMemories() {
        runInBackground(() -> db.memoryEntryDao().deleteAll());
    }

    // ==================== Lifecycle ====================

    void clear() {
        clearMessages();
        clearMemories();
    }

    void shutdown() {
        handlerThread.quitSafely();
    }

    // ==================== Internal ====================

    private void runInBackground(Runnable task) {
        storeHandler.post(task);
    }

    private void recordMemoryAccess(long id) {
        runInBackground(() -> db.memoryEntryDao().recordAccess(id, System.currentTimeMillis()));
    }

    private static int aiMessageTypeToInt(com.chatai.aiinteract.models.AiMessageType type) {
        switch (type) {
            case VOICE:  return 7; // RECEIVE_VOICE
            case VIDEO:  return 9; // RECEIVE_VIDEO
            default:     return 0; // RECEIVE_TEXT
        }
    }
}
