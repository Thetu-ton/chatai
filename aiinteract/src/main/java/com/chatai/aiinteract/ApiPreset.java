package com.chatai.aiinteract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Predefined AI API provider presets.
 * Users can select a preset, enter their API key, and start chatting.
 * The "CUSTOM" preset allows fully manual configuration.
 */
public enum ApiPreset {

    GROK(
        "grok",
        "Grok (xAI)",
        "https://api.x.ai/v1/chat/completions",
        "grok-3",
        Arrays.asList("grok-3", "grok-3-mini"),
        "https://console.x.ai",
        "xAI 开发的 Grok 模型，由 Elon Musk 团队打造，支持文本、语音、视频回复"
    ),

    OPENAI(
        "openai",
        "OpenAI (ChatGPT)",
        "https://api.openai.com/v1/chat/completions",
        "gpt-4o",
        Arrays.asList("gpt-4o", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo"),
        "https://platform.openai.com/api-keys",
        "OpenAI 的 GPT 系列模型，最广泛使用的 AI 对话接口"
    ),

    CLAUDE(
        "claude",
        "Claude (Anthropic)",
        "https://api.anthropic.com/v1/messages",
        "claude-opus-4-7",
        Arrays.asList("claude-opus-4-7", "claude-sonnet-4-6", "claude-haiku-4-5"),
        "https://console.anthropic.com",
        "Anthropic 的 Claude 模型，擅长长文本理解与安全回复"
    ),

    CUSTOM(
        "custom",
        "自定义 API",
        "",
        "",
        new ArrayList<>(),
        "",
        "手动输入 API 地址和密钥，适配任意兼容 OpenAI 格式的接口"
    );

    private final String id;
    private final String displayName;
    private final String endpoint;
    private final String defaultModel;
    private final List<String> models;
    private final String apiKeyUrl;
    private final String description;

    ApiPreset(String id, String displayName, String endpoint, String defaultModel,
              List<String> models, String apiKeyUrl, String description) {
        this.id = id;
        this.displayName = displayName;
        this.endpoint = endpoint;
        this.defaultModel = defaultModel;
        this.models = models;
        this.apiKeyUrl = apiKeyUrl;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEndpoint() { return endpoint; }
    public String getDefaultModel() { return defaultModel; }
    public List<String> getModels() { return models; }
    public String getApiKeyUrl() { return apiKeyUrl; }
    public String getDescription() { return description; }

    /**
     * Create an AiConfig from this preset with the given API key.
     */
    public AiConfig.Builder createConfigBuilder(String apiKey) {
        return new AiConfig.Builder(this.endpoint, apiKey)
                .model(this.defaultModel);
    }

    /**
     * Find a preset by its id string. Returns CUSTOM if not found.
     */
    public static ApiPreset fromId(String id) {
        for (ApiPreset preset : values()) {
            if (preset.id.equals(id)) return preset;
        }
        return CUSTOM;
    }

    /**
     * Get all presets except CUSTOM.
     */
    public static List<ApiPreset> getBuiltInPresets() {
        List<ApiPreset> list = new ArrayList<>();
        for (ApiPreset preset : values()) {
            if (preset != CUSTOM) list.add(preset);
        }
        return list;
    }
}
