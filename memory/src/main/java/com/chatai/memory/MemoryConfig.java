package com.chatai.memory;

/**
 * Configuration for the Memory module.
 *
 * <p>Controls conversation storage, context window size, long-term
 * memory recall via Mem0, and the system prompt injected into LLM requests.
 */
public class MemoryConfig {

    private final int maxContextMessages;
    private final int maxSearchResults;
    private final int maxMemoryEntries;
    private final boolean persistEnabled;
    private final String systemPrompt;
    private final String aiUserId;
    private final String aiUserName;
    private final String userDisplayName;

    // Mem0 settings
    private final String mem0Endpoint;
    private final String mem0ApiKey;
    private final String mem0UserId;
    private final boolean mem0Enabled;

    private MemoryConfig(Builder builder) {
        this.maxContextMessages = builder.maxContextMessages;
        this.maxSearchResults = builder.maxSearchResults;
        this.maxMemoryEntries = builder.maxMemoryEntries;
        this.persistEnabled = builder.persistEnabled;
        this.systemPrompt = builder.systemPrompt;
        this.aiUserId = builder.aiUserId;
        this.aiUserName = builder.aiUserName;
        this.userDisplayName = builder.userDisplayName;
        this.mem0Endpoint = builder.mem0Endpoint;
        this.mem0ApiKey = builder.mem0ApiKey;
        this.mem0UserId = builder.mem0UserId;
        this.mem0Enabled = builder.mem0Enabled;
    }

    public int getMaxContextMessages() { return maxContextMessages; }
    public int getMaxSearchResults() { return maxSearchResults; }
    public int getMaxMemoryEntries() { return maxMemoryEntries; }
    public boolean isPersistEnabled() { return persistEnabled; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getAiUserId() { return aiUserId; }
    public String getAiUserName() { return aiUserName; }
    public String getUserDisplayName() { return userDisplayName; }

    // Mem0 getters
    public String getMem0Endpoint() { return mem0Endpoint; }
    public String getMem0ApiKey() { return mem0ApiKey; }
    public String getMem0UserId() { return mem0UserId; }
    public boolean isMem0Enabled() { return mem0Enabled; }

    public static class Builder {
        // Local settings
        private int maxContextMessages = 50;
        private int maxSearchResults = 10;
        private int maxMemoryEntries = 5;
        private boolean persistEnabled = true;
        private String systemPrompt =
            "You are a helpful AI assistant in a chat application. " +
            "Use the conversation context and long-term memories provided to give " +
            "relevant, personalized responses. If the user references earlier topics, " +
            "use the long-term memory to recall details about them.";
        private String aiUserId = "ai_user";
        private String aiUserName = "AI Assistant";
        private String userDisplayName = "User";

        // Mem0 settings
        private String mem0Endpoint;
        private String mem0ApiKey;
        private String mem0UserId;
        private boolean mem0Enabled = false;

        public Builder() {}

        /** Max recent messages to include as LLM context. Default: 50. */
        public Builder maxContextMessages(int max) { this.maxContextMessages = max; return this; }

        /** Max keyword-search results to include. Default: 10. */
        public Builder maxSearchResults(int max) { this.maxSearchResults = max; return this; }

        /** Max long-term memory entries to inject into the system prompt. Default: 5. */
        public Builder maxMemoryEntries(int max) { this.maxMemoryEntries = max; return this; }

        /** Enable/disable local persistence. Default: true. */
        public Builder persistEnabled(boolean enabled) { this.persistEnabled = enabled; return this; }

        /** Custom system prompt. */
        public Builder systemPrompt(String prompt) { this.systemPrompt = prompt; return this; }

        public Builder aiUserId(String id) { this.aiUserId = id; return this; }
        public Builder aiUserName(String name) { this.aiUserName = name; return this; }
        public Builder userDisplayName(String name) { this.userDisplayName = name; return this; }

        /**
         * Configure Mem0 for long-term memory.
         *
         * @param endpoint Mem0 server URL (e.g., "https://api.mem0.ai")
         * @param apiKey   Mem0 API key
         * @param userId   Unique user identifier for memory scoping
         */
        public Builder mem0(String endpoint, String apiKey, String userId) {
            this.mem0Endpoint = endpoint;
            this.mem0ApiKey = apiKey;
            this.mem0UserId = userId;
            this.mem0Enabled = true;
            return this;
        }

        /**
         * Set Mem0 endpoint separately (if not using {@link #mem0}).
         */
        public Builder mem0Endpoint(String endpoint) {
            this.mem0Endpoint = endpoint;
            this.mem0Enabled = endpoint != null && !endpoint.isEmpty();
            return this;
        }

        public Builder mem0ApiKey(String apiKey) { this.mem0ApiKey = apiKey; return this; }
        public Builder mem0UserId(String userId) { this.mem0UserId = userId; return this; }

        public MemoryConfig build() {
            return new MemoryConfig(this);
        }
    }
}
