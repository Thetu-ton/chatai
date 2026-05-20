package com.chatai.memory;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.chatai.aiinteract.AiInteract;
import com.chatai.aiinteract.models.AiMessage;
import com.chatai.memory.mem0.Mem0Client;
import com.chatai.memory.mem0.Mem0Models;
import com.chatai.memory.storage.MemoryEntryEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jiguang.imui.commons.models.IMessage;

/**
 * Core orchestrator for the memory module.
 *
 * <p>Sits between the UI layer (IMessage) and the AI layer (AiInteract/AiMessage).
 *
 * <p>Two-tier memory architecture:
 * <ul>
 *   <li><b>Local (Room/SQLite)</b> — raw conversation messages, FTS4 keyword search</li>
 *   <li><b>Cloud (Mem0)</b> — long-term semantic memories via vector embedding search</li>
 * </ul>
 *
 * <p>Full lifecycle:
 * <pre>
 *   MemoryConfig config = new MemoryConfig.Builder()
 *       .mem0("https://api.mem0.ai", "key", "user123")
 *       .build();
 *   MemoryManager.init(context, config);
 *   AiInteract.init(aiConfig);
 *   MemoryManager.getInstance().restoreConversationToAiInteract();
 * </pre>
 */
public class MemoryManager {

    private static MemoryManager instance;

    private MemoryConfig config;
    private MemoryStore store;
    private Mem0Client mem0;
    private final Handler mainHandler;

    private MemoryManager() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialize the memory module.
     *
     * @param context Application context (for Room database)
     * @param config  Memory configuration (including Mem0 settings)
     */
    public static synchronized void init(Context context, MemoryConfig config) {
        if (instance == null) {
            instance = new MemoryManager();
        }
        instance.config = config;
        instance.store = new MemoryStore(context.getApplicationContext());

        if (config.isMem0Enabled()) {
            instance.mem0 = new Mem0Client(
                config.getMem0Endpoint(),
                config.getMem0ApiKey(),
                config.getMem0UserId()
            );
        }
    }

    public static MemoryManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "MemoryManager not initialized. Call init(context, config) first.");
        }
        return instance;
    }

    // ==================== Message Ingestion ====================

    /**
     * Store a user message in local persistent storage.
     */
    public void onUserMessage(IMessage message, String senderId) {
        AiMessage aiMsg = MessageConverter.toAiMessage(message, senderId);
        store.addAiMessage(aiMsg);
    }

    /**
     * Store an AI response in local persistent storage.
     */
    public void onAiResponse(AiMessage aiMessage) {
        store.addAiMessage(aiMessage);
    }

    /**
     * Send user text to Mem0 for long-term memory extraction.
     * Mem0 automatically extracts, deduplicates, and consolidates memories.
     *
     * <p>Call this alongside {@link #onUserMessage} when the user sends a message.
     * This is async and does not block the UI.
     */
    public void memorize(String text) {
        if (mem0 == null) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "chat");
        metadata.put("app", "chatai");

        mem0.addMemory(text, metadata, new Mem0Client.Callback<List<Mem0Models.MemoryItem>>() {
            @Override
            public void onSuccess(List<Mem0Models.MemoryItem> memories) {
                // Memories are now stored in Mem0 — they will be injected
                // into the system prompt on the next LLM call.
            }

            @Override
            public void onError(int code, String message) {
                android.util.Log.w("MemoryManager", "Mem0 add failed: " + message);
            }
        });
    }

    // ==================== Context Assembly for LLM ====================

    /**
     * Build the system prompt with Mem0 long-term memories injected.
     *
     * <p>Call this before sending to the AI to ensure the LLM has
     * access to the user's long-term memory.
     *
     * <p>For async Mem0 retrieval, use {@link #buildSystemPromptAsync}.
     */
    public String buildSystemPromptWithMemories() {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getSystemPrompt());

        String localMemory = store.buildMemoryContext(config.getMaxMemoryEntries());
        if (!localMemory.isEmpty()) {
            sb.append(localMemory);
        }

        sb.append("\n\nWhen the user references past topics, use the memories above to personalize your response.");

        return sb.toString();
    }

    /**
     * Build system prompt asynchronously, fetching latest memories from Mem0 first.
     * Use this when you want fresh Mem0 data before every LLM call.
     *
     * @param callback Receives the assembled system prompt string
     */
    public void buildSystemPromptAsync(SystemPromptCallback callback) {
        if (mem0 == null) {
            callback.onReady(buildSystemPromptWithMemories());
            return;
        }

        mem0.searchMemories("recent important user information",
            config.getMaxMemoryEntries(),
            new Mem0Client.Callback<List<Mem0Models.MemoryItem>>() {
                @Override
                public void onSuccess(List<Mem0Models.MemoryItem> results) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(config.getSystemPrompt());
                    sb.append("\n\n[Long-term memories about the user — use these to personalize responses]\n");

                    for (Mem0Models.MemoryItem item : results) {
                        sb.append("- ").append(item.memory).append("\n");
                    }

                    // Also include local memories
                    String local = store.buildMemoryContext(config.getMaxMemoryEntries());
                    if (!local.isEmpty()) {
                        sb.append(local);
                    }

                    sb.append("\nWhen the user references past topics, use the memories above to personalize your response.");

                    mainHandler.post(() -> callback.onReady(sb.toString()));
                }

                @Override
                public void onError(int code, String message) {
                    // Fall back to local-only on Mem0 error
                    mainHandler.post(() -> callback.onReady(buildSystemPromptWithMemories()));
                }
            });
    }

    /** Callback for async system prompt assembly. */
    public interface SystemPromptCallback {
        void onReady(String systemPrompt);
    }

    /**
     * Assemble conversation context for the LLM.
     * Combines recent messages + keyword-matched history.
     * Long-term memories go in the system prompt via buildSystemPromptWithMemories().
     */
    public List<AiMessage> getContextForLLM(String currentUserInput) {
        int maxContext = config.getMaxContextMessages();
        int maxSearch = config.getMaxSearchResults();

        List<AiMessage> recent = store.getRecentContext(maxContext);

        String keywords = extractKeywords(currentUserInput);
        List<AiMessage> related = keywords.isEmpty()
            ? new ArrayList<>()
            : store.searchExcludingRecent(keywords, maxContext, maxSearch);

        Set<AiMessage> merged = new LinkedHashSet<>();
        merged.addAll(recent);
        merged.addAll(related);

        List<AiMessage> context = new ArrayList<>();
        context.add(AiMessage.text(AiMessage.Role.SYSTEM, buildSystemPromptWithMemories()));
        context.addAll(merged);

        return context;
    }

    /**
     * Get only recent messages (no keyword search, no async Mem0).
     */
    public List<AiMessage> getRecentContext() {
        List<AiMessage> context = new ArrayList<>();
        context.add(AiMessage.text(AiMessage.Role.SYSTEM, buildSystemPromptWithMemories()));
        context.addAll(store.getRecentContext(config.getMaxContextMessages()));
        return context;
    }

    // ==================== Local Message Search (FTS4) ====================

    /**
     * Full-text search across stored conversation messages (local Room).
     */
    public List<AiMessage> searchMessages(String query) {
        return store.searchMessages(query, config.getMaxSearchResults());
    }

    /**
     * Full-text search returning display-ready IMessage objects.
     */
    public List<IMessage> searchMessagesForDisplay(String query,
                                                   cn.jiguang.imui.commons.models.IUser aiUser) {
        List<AiMessage> results = searchMessages(query);
        List<IMessage> displayResults = new ArrayList<>();
        for (AiMessage msg : results) {
            boolean isReceived = msg.getRole() == AiMessage.Role.ASSISTANT;
            displayResults.add(MessageConverter.toDisplayMessage(msg, aiUser, isReceived));
        }
        return displayResults;
    }

    // ==================== Long-term Memory (Mem0 Cloud) ====================

    /**
     * Store a local long-term memory entry. If Mem0 is enabled, also syncs to Mem0.
     */
    public void remember(String key, String value, String summary, int importance) {
        store.putMemory(key, value, summary, Math.min(10, Math.max(0, importance)));

        // Also sync to Mem0 for semantic search
        if (mem0 != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", key);
            metadata.put("summary", summary);
            metadata.put("importance", importance);
            mem0.addMemory(value, metadata, null);
        }
    }

    /**
     * Recall a local memory entry by key.
     */
    public MemoryEntryEntity recall(String key) {
        return store.getMemory(key);
    }

    /**
     * Search long-term memories in Mem0 (semantic search).
     * Falls back to local search if Mem0 is not configured.
     */
    public void searchLongTermMemory(String query, Mem0Callback callback) {
        if (mem0 != null) {
            mem0.searchMemories(query, config.getMaxSearchResults(),
                new Mem0Client.Callback<List<Mem0Models.MemoryItem>>() {
                    @Override
                    public void onSuccess(List<Mem0Models.MemoryItem> results) {
                        List<MemoryEntry> entries = new ArrayList<>();
                        for (Mem0Models.MemoryItem item : results) {
                            MemoryEntry entry = new MemoryEntry(
                                item.id, item.memory,
                                item.metadata != null ? String.valueOf(item.metadata.getOrDefault("summary", "")) : "",
                                item.metadata != null && item.metadata.containsKey("importance")
                                    ? ((Number) item.metadata.get("importance")).intValue() : 5,
                                item.score != null ? item.score : 0f
                            );
                            entries.add(entry);
                        }
                        mainHandler.post(() -> callback.onSuccess(entries));
                    }

                    @Override
                    public void onError(int code, String message) {
                        mainHandler.post(() -> callback.onError(code, message));
                    }
                });
        } else {
            // Fall back to local keyword search
            List<MemoryEntryEntity> local = store.searchMemories(query, config.getMaxSearchResults());
            List<MemoryEntry> entries = new ArrayList<>();
            for (MemoryEntryEntity e : local) {
                entries.add(new MemoryEntry(
                    String.valueOf(e.getId()), e.getMemoryValue(),
                    e.getSummary(), e.getImportance(), 0f
                ));
            }
            callback.onSuccess(entries);
        }
    }

    /**
     * Get all memories from Mem0.
     */
    public void getAllMemories(Mem0Callback callback) {
        if (mem0 != null) {
            mem0.getAllMemories(new Mem0Client.Callback<List<Mem0Models.MemoryItem>>() {
                @Override
                public void onSuccess(List<Mem0Models.MemoryItem> results) {
                    List<MemoryEntry> entries = new ArrayList<>();
                    for (Mem0Models.MemoryItem item : results) {
                        entries.add(new MemoryEntry(
                            item.id, item.memory,
                            item.metadata != null ? String.valueOf(item.metadata.getOrDefault("summary", "")) : "",
                            item.metadata != null && item.metadata.containsKey("importance")
                                ? ((Number) item.metadata.get("importance")).intValue() : 5,
                            0f
                        ));
                    }
                    mainHandler.post(() -> callback.onSuccess(entries));
                }

                @Override
                public void onError(int code, String message) {
                    mainHandler.post(() -> callback.onError(code, message));
                }
            });
        } else {
            callback.onSuccess(new ArrayList<>());
        }
    }

    /**
     * Delete a memory from Mem0 by ID.
     */
    public void forgetMem0(String memoryId) {
        if (mem0 != null) {
            mem0.deleteMemory(memoryId, null);
        }
    }

    /**
     * Forget a local memory entry by key.
     */
    public void forget(String key) {
        store.deleteMemory(key);
    }

    /**
     * Get all local memory entries.
     */
    public List<MemoryEntryEntity> getAllLocalMemories() {
        return store.getTopMemories(Integer.MAX_VALUE);
    }

    /** Callback for long-term memory operations. */
    public interface Mem0Callback {
        void onSuccess(List<MemoryEntry> memories);
        void onError(int code, String message);
    }

    /**
     * A display-friendly memory entry returned from Mem0 or local search.
     */
    public static class MemoryEntry {
        private final String id;
        private final String value;
        private final String summary;
        private final int importance;
        private final float score;

        public MemoryEntry(String id, String value, String summary, int importance, float score) {
            this.id = id;
            this.value = value;
            this.summary = summary;
            this.importance = importance;
            this.score = score;
        }

        public String getId() { return id; }
        public String getValue() { return value; }
        public String getSummary() { return summary; }
        public int getImportance() { return importance; }
        public float getScore() { return score; }

        @Override
        public String toString() {
            return value;
        }
    }

    // ==================== Conversation Management ====================

    public int getMessageCount() { return store.getMessageCount(); }
    public int getMemoryCount() { return store.getMemoryCount(); }

    /**
     * Clear all local data. Mem0 data is preserved (use forgetMem0 to delete).
     */
    public void clearAll() {
        store.clear();
        AiInteract.getInstance().clearHistory();
    }

    /**
     * Restore persisted conversation into AiInteract.
     * Call after initializing both modules.
     */
    public void restoreConversationToAiInteract() {
        List<AiMessage> recent = store.getRecentContext(config.getMaxContextMessages());
        List<AiMessage> history = AiInteract.getInstance().getConversationHistory();
        history.clear();
        history.addAll(recent);
        AiInteract.getInstance().setSystemPrompt(buildSystemPromptWithMemories());
    }

    /**
     * Check if Mem0 long-term memory is available.
     */
    public boolean isMem0Available() {
        return mem0 != null;
    }

    /**
     * Release resources.
     */
    public void shutdown() {
        if (store != null) {
            store.shutdown();
        }
        if (mem0 != null) {
            mem0.shutdown();
        }
        instance = null;
    }

    public MemoryConfig getConfig() { return config; }

    // ==================== Internal ====================

    private String extractKeywords(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String[] words = input.split("[\\s，,。.!！?？、：:；;]+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() >= 2) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
        }
        return sb.toString();
    }
}
