package com.chatai.memory;

import com.chatai.aiinteract.ApiPreset;

/**
 * Configuration for the Memory module.
 *
 * <p>Controls conversation storage, context window size, long-term
 * memory recall via Mem0, and the system prompt injected into LLM requests.
 *
 * <p>Supports AI provider presets from the aiinteract module:
 * <pre>
 *   MemoryConfig config = MemoryConfig.fromPreset(ApiPreset.GROK, "xai-key", "user_123")
 *       .mem0("https://api.mem0.ai", "mem0-key")
 *       .build();
 * </pre>
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

    // AI provider preset
    private final ApiPreset preset;
    private final String aiApiEndpoint;
    private final String aiApiKey;
    private final String aiModel;

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
        this.preset = builder.preset;
        this.aiApiEndpoint = builder.aiApiEndpoint;
        this.aiApiKey = builder.aiApiKey;
        this.aiModel = builder.aiModel;
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

    // AI provider getters
    public ApiPreset getPreset() { return preset; }
    public String getAiApiEndpoint() { return aiApiEndpoint; }
    public String getAiApiKey() { return aiApiKey; }
    public String getAiModel() { return aiModel; }

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

        // AI provider settings (if not using AiConfig directly)
        private ApiPreset preset;
        private String aiApiEndpoint;
        private String aiApiKey;
        private String aiModel;

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
         */
        public Builder mem0(String endpoint, String apiKey) {
            this.mem0Endpoint = endpoint;
            this.mem0ApiKey = apiKey;
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

        /**
         * Set the AI provider from an ApiPreset.
         * This pre-fills the AI endpoint, model, and API key in one call.
         *
         * @param preset The AI provider preset (GROK, OPENAI, CLAUDE, CUSTOM)
         * @param apiKey The API key for the provider
         */
        public Builder aiPreset(ApiPreset preset, String apiKey) {
            this.preset = preset;
            this.aiApiKey = apiKey;
            if (preset != ApiPreset.CUSTOM) {
                this.aiApiEndpoint = preset.getEndpoint();
                this.aiModel = preset.getDefaultModel();
            }
            return this;
        }

        /**
         * Override the model from the preset's default.
         */
        public Builder aiModel(String model) { this.aiModel = model; return this; }

        /**
         * Set a custom AI API endpoint (overrides preset endpoint).
         */
        public Builder aiApiEndpoint(String endpoint) { this.aiApiEndpoint = endpoint; return this; }

        /**
         * Set the AI API key directly (alternative to aiPreset).
         */
        public Builder aiApiKey(String apiKey) { this.aiApiKey = apiKey; return this; }

        public MemoryConfig build() {
            return new MemoryConfig(this);
        }
    }

    /**
     * Create a Builder pre-filled with an AI provider preset.
     *
     * <p>This is the recommended way to initialize:
     * <pre>
     *   MemoryConfig config = MemoryConfig.fromPreset(ApiPreset.GROK, "xai-key", "user_123")
     *       .mem0("https://api.mem0.ai", "mem0-key")
     *       .build();
     * </pre>
     *
     * @param preset   The AI provider (GROK, OPENAI, CLAUDE)
     * @param apiKey   API key for the provider
     * @param mem0UserId User ID for Mem0 memory scoping
     * @return A pre-populated Builder
     */
    public static Builder fromPreset(ApiPreset preset, String apiKey, String mem0UserId) {
        Builder builder = new Builder();
        builder.preset = preset;
        builder.aiApiKey = apiKey;
        builder.mem0UserId = mem0UserId;
        if (preset != ApiPreset.CUSTOM) {
            builder.aiApiEndpoint = preset.getEndpoint();
            builder.aiModel = preset.getDefaultModel();
        }
        return builder;
    }
}
