package com.chatai.aiinteract;

/**
 * Configuration for the AI interaction module.
 * Set API endpoint, keys, model, and feature flags.
 */
public class AiConfig {

    private String apiEndpoint;
    private String apiKey;
    private String model;
    private boolean voiceEnabled;
    private boolean videoEnabled;
    private int maxTokens;
    private float temperature;
    private String aiUserName;
    private String aiUserAvatar;
    private String aiUserId;
    private String mediaDownloadDir;

    private ApiPreset preset;

    private AiConfig(Builder builder) {
        this.apiEndpoint = builder.apiEndpoint;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.voiceEnabled = builder.voiceEnabled;
        this.videoEnabled = builder.videoEnabled;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.aiUserName = builder.aiUserName;
        this.aiUserAvatar = builder.aiUserAvatar;
        this.aiUserId = builder.aiUserId;
        this.mediaDownloadDir = builder.mediaDownloadDir;
        this.preset = builder.preset;
    }

    /**
     * Create a config from a preset with the given API key.
     */
    public static Builder fromPreset(ApiPreset preset, String apiKey) {
        return new Builder(preset.getEndpoint(), apiKey)
                .model(preset.getDefaultModel())
                .preset(preset);
    }

    public String getApiEndpoint() { return apiEndpoint; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public boolean isVoiceEnabled() { return voiceEnabled; }
    public boolean isVideoEnabled() { return videoEnabled; }
    public int getMaxTokens() { return maxTokens; }
    public float getTemperature() { return temperature; }
    public String getAiUserName() { return aiUserName; }
    public String getAiUserAvatar() { return aiUserAvatar; }
    public String getAiUserId() { return aiUserId; }
    public String getMediaDownloadDir() { return mediaDownloadDir; }
    public ApiPreset getPreset() { return preset; }

    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setModel(String model) { this.model = model; }

    public static class Builder {
        private String apiEndpoint;
        private String apiKey;
        private String model = "gpt-4";
        private boolean voiceEnabled = true;
        private boolean videoEnabled = false;
        private int maxTokens = 2048;
        private float temperature = 0.7f;
        private String aiUserName = "AI Assistant";
        private String aiUserAvatar = "";
        private String aiUserId = "ai_user";
        private String mediaDownloadDir;
        private ApiPreset preset;

        public Builder(String apiEndpoint, String apiKey) {
            this.apiEndpoint = apiEndpoint;
            this.apiKey = apiKey;
        }

        public Builder model(String model) { this.model = model; return this; }
        public Builder voiceEnabled(boolean enabled) { this.voiceEnabled = enabled; return this; }
        public Builder videoEnabled(boolean enabled) { this.videoEnabled = enabled; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder temperature(float temperature) { this.temperature = temperature; return this; }
        public Builder aiUserName(String name) { this.aiUserName = name; return this; }
        public Builder aiUserAvatar(String avatar) { this.aiUserAvatar = avatar; return this; }
        public Builder aiUserId(String id) { this.aiUserId = id; return this; }
        public Builder mediaDownloadDir(String dir) { this.mediaDownloadDir = dir; return this; }
        public Builder preset(ApiPreset preset) { this.preset = preset; return this; }

        public AiConfig build() {
            if (apiEndpoint == null || apiEndpoint.isEmpty()) {
                throw new IllegalArgumentException("apiEndpoint must not be null or empty");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey must not be null or empty");
            }
            return new AiConfig(this);
        }
    }
}
