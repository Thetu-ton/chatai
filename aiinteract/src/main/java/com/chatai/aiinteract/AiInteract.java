package com.chatai.aiinteract;

import android.os.Handler;
import android.os.Looper;

import com.chatai.aiinteract.callback.AiCallback;
import com.chatai.aiinteract.models.AiMessage;
import com.chatai.aiinteract.models.AiMessageType;
import com.chatai.aiinteract.network.AiApiClient;
import com.chatai.aiinteract.util.AiFileDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for AI interaction.
 * Singleton that manages AI API communication, message history, and media downloads.
 *
 * <p>Usage:
 * <pre>
 *   // 1. Initialize
 *   AiConfig config = new AiConfig.Builder("https://api.openai.com/v1/chat/completions", "sk-...")
 *       .model("gpt-4")
 *       .voiceEnabled(true)
 *       .build();
 *   AiInteract.init(config);
 *
 *   // 2. Send messages
 *   AiInteract.getInstance().sendTextMessage("Hello AI!", new AiCallback() { ... });
 *   AiInteract.getInstance().sendVoiceMessage("/path/to/audio.m4a", new AiCallback() { ... });
 * </pre>
 */
public class AiInteract {

    private static AiInteract instance;

    private AiConfig config;
    private AiApiClient apiClient;
    private AiFileDownloader fileDownloader;
    private final List<AiMessage> conversationHistory;
    private final Handler mainHandler;
    private String systemPromptOverride;

    private AiInteract() {
        this.conversationHistory = new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialize the AI interaction module with configuration.
     * Must be called once before using any other methods.
     */
    public static synchronized void init(AiConfig config) {
        if (instance == null) {
            instance = new AiInteract();
        }
        instance.config = config;
        instance.systemPromptOverride = null;
        instance.apiClient = new AiApiClient(config);
        instance.fileDownloader = new AiFileDownloader();
        instance.conversationHistory.clear();
    }

    /**
     * Get the singleton instance. init() must be called first.
     */
    public static AiInteract getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AiInteract not initialized. Call AiInteract.init(config) first.");
        }
        return instance;
    }

    /**
     * Update configuration at runtime (e.g., change API key or model).
     */
    public void updateConfig(AiConfig newConfig) {
        this.config = newConfig;
        if (this.apiClient != null) {
            this.apiClient.shutdown();
        }
        this.apiClient = systemPromptOverride != null
            ? new AiApiClient(newConfig, systemPromptOverride)
            : new AiApiClient(newConfig);
    }

    /**
     * Get current configuration.
     */
    public AiConfig getConfig() {
        return config;
    }

    // ==================== Send Messages ====================

    /**
     * Send a text message to the AI.
     *
     * @param text     The user's text input
     * @param callback Callback to receive the AI response
     */
    public void sendTextMessage(String text, AiCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            notifyError(callback, -3, "Text message cannot be empty");
            return;
        }

        AiMessage userMessage = AiMessage.text(AiMessage.Role.USER, text);
        conversationHistory.add(userMessage);

        apiClient.sendMessage(new ArrayList<>(conversationHistory), new AiCallback() {
            @Override
            public void onResponse(AiMessage response) {
                conversationHistory.add(response);
                downloadMediaIfNeeded(response, callback);
            }

            @Override
            public void onStreamChunk(String chunk) {
                notifyStreamChunk(callback, chunk);
            }

            @Override
            public void onStreamComplete() {
                notifyStreamComplete(callback);
            }

            @Override
            public void onError(int code, String message) {
                notifyError(callback, code, message);
            }

            @Override
            public void onFileDownloadProgress(int progress) {
                notifyFileProgress(callback, progress);
            }
        });
    }

    /**
     * Send a text message with streaming response.
     * The callback will receive onStreamChunk for each text fragment.
     */
    public void sendTextMessageStreaming(String text, AiCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            notifyError(callback, -3, "Text message cannot be empty");
            return;
        }

        AiMessage userMessage = AiMessage.text(AiMessage.Role.USER, text);
        conversationHistory.add(userMessage);

        apiClient.sendMessageStreaming(new ArrayList<>(conversationHistory), new AiCallback() {
            @Override
            public void onResponse(AiMessage response) {
                conversationHistory.add(response);
                notifyResponse(callback, response);
            }

            @Override
            public void onStreamChunk(String chunk) {
                notifyStreamChunk(callback, chunk);
            }

            @Override
            public void onStreamComplete() {
                notifyStreamComplete(callback);
            }

            @Override
            public void onError(int code, String message) {
                notifyError(callback, code, message);
            }

            @Override
            public void onFileDownloadProgress(int progress) {
                notifyFileProgress(callback, progress);
            }
        });
    }

    /**
     * Send a voice message to the AI.
     *
     * @param audioFilePath Local path to the recorded audio file
     * @param callback      Callback to receive the AI response
     */
    public void sendVoiceMessage(String audioFilePath, AiCallback callback) {
        if (audioFilePath == null || audioFilePath.isEmpty()) {
            notifyError(callback, -3, "Audio file path cannot be empty");
            return;
        }

        AiMessage userMessage = AiMessage.voice(AiMessage.Role.USER, "[Voice message]", audioFilePath);
        userMessage.setLocalMediaPath(audioFilePath);
        conversationHistory.add(userMessage);

        apiClient.sendMessage(new ArrayList<>(conversationHistory), new AiCallback() {
            @Override
            public void onResponse(AiMessage response) {
                conversationHistory.add(response);
                downloadMediaIfNeeded(response, callback);
            }

            @Override
            public void onStreamChunk(String chunk) {
                notifyStreamChunk(callback, chunk);
            }

            @Override
            public void onStreamComplete() {
                notifyStreamComplete(callback);
            }

            @Override
            public void onError(int code, String message) {
                notifyError(callback, code, message);
            }

            @Override
            public void onFileDownloadProgress(int progress) {
                notifyFileProgress(callback, progress);
            }
        });
    }

    /**
     * Send a video message to the AI.
     *
     * @param videoFilePath Local path to the video file
     * @param callback      Callback to receive the AI response
     */
    public void sendVideoMessage(String videoFilePath, AiCallback callback) {
        if (videoFilePath == null || videoFilePath.isEmpty()) {
            notifyError(callback, -3, "Video file path cannot be empty");
            return;
        }

        AiMessage userMessage = AiMessage.video(AiMessage.Role.USER, "[Video message]", videoFilePath);
        userMessage.setLocalMediaPath(videoFilePath);
        conversationHistory.add(userMessage);

        apiClient.sendMessage(new ArrayList<>(conversationHistory), new AiCallback() {
            @Override
            public void onResponse(AiMessage response) {
                conversationHistory.add(response);
                downloadMediaIfNeeded(response, callback);
            }

            @Override
            public void onStreamChunk(String chunk) {
                notifyStreamChunk(callback, chunk);
            }

            @Override
            public void onStreamComplete() {
                notifyStreamComplete(callback);
            }

            @Override
            public void onError(int code, String message) {
                notifyError(callback, code, message);
            }

            @Override
            public void onFileDownloadProgress(int progress) {
                notifyFileProgress(callback, progress);
            }
        });
    }

    // ==================== Conversation Management ====================

    /**
     * Get the full conversation history.
     */
    public List<AiMessage> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * Clear conversation history and start a new chat.
     */
    public void clearHistory() {
        conversationHistory.clear();
    }

    /**
     * Add a system message to control AI behavior.
     */
    public void setSystemPrompt(String prompt) {
        this.systemPromptOverride = prompt;
        conversationHistory.removeIf(msg -> msg.getRole() == AiMessage.Role.SYSTEM);
        conversationHistory.add(0, AiMessage.text(AiMessage.Role.SYSTEM, prompt));
        if (apiClient != null) {
            apiClient.shutdown();
        }
        apiClient = new AiApiClient(config, prompt);
    }

    // ==================== Preset Switching ====================

    /**
     * Switch to a different API preset at runtime.
     * Preserves conversation history when switching.
     *
     * @param preset The new API preset
     * @param apiKey The API key for the new provider
     */
    public void switchToPreset(ApiPreset preset, String apiKey) {
        AiConfig newConfig = AiConfig.fromPreset(preset, apiKey)
                .aiUserName(config.getAiUserName())
                .aiUserAvatar(config.getAiUserAvatar())
                .aiUserId(config.getAiUserId())
                .mediaDownloadDir(config.getMediaDownloadDir())
                .voiceEnabled(config.isVoiceEnabled())
                .videoEnabled(config.isVideoEnabled())
                .maxTokens(config.getMaxTokens())
                .temperature(config.getTemperature())
                .build();
        updateConfig(newConfig);
    }

    /**
     * Get all available AI provider presets for the UI to display.
     */
    public static List<ApiPreset> getAvailablePresets() {
        return ApiPreset.getBuiltInPresets();
    }

    // ==================== Shutdown ====================

    /**
     * Release resources. Call when the app is shutting down.
     */
    public void shutdown() {
        if (apiClient != null) {
            apiClient.shutdown();
        }
        if (fileDownloader != null) {
            fileDownloader.shutdown();
        }
        conversationHistory.clear();
        instance = null;
    }

    // ==================== Internal ====================

    private void downloadMediaIfNeeded(AiMessage response, AiCallback callback) {
        String mediaUrl = response.getMediaUrl();
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            // Pure text response, deliver immediately
            notifyResponse(callback, response);
            return;
        }

        String downloadDir = config.getMediaDownloadDir();
        if (downloadDir == null || downloadDir.isEmpty()) {
            downloadDir = AiFileDownloader.getDefaultDownloadDir();
        }

        String suffix = response.getMessageType() == AiMessageType.VIDEO ? "mp4" : "mp3";

        fileDownloader.download(mediaUrl, downloadDir, suffix, new AiFileDownloader.DownloadCallback() {
            @Override
            public void onProgress(int progress) {
                notifyFileProgress(callback, progress);
            }

            @Override
            public void onSuccess(String localFilePath) {
                response.setLocalMediaPath(localFilePath);
                notifyResponse(callback, response);
            }

            @Override
            public void onError(String error) {
                // If media download fails, still deliver text content
                notifyResponse(callback, response);
            }
        });
    }

    // ==================== Main-thread dispatch ====================

    private void notifyResponse(AiCallback callback, AiMessage message) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onResponse(message);
            }
        });
    }

    private void notifyStreamChunk(AiCallback callback, String chunk) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onStreamChunk(chunk);
            }
        });
    }

    private void notifyStreamComplete(AiCallback callback) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onStreamComplete();
            }
        });
    }

    private void notifyError(AiCallback callback, int code, String message) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(code, message);
            }
        });
    }

    private void notifyFileProgress(AiCallback callback, int progress) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFileDownloadProgress(progress);
            }
        });
    }
}
