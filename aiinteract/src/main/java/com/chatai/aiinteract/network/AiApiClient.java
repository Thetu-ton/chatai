package com.chatai.aiinteract.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.chatai.aiinteract.AiConfig;
import com.chatai.aiinteract.callback.AiCallback;
import com.chatai.aiinteract.models.AiMessage;
import com.chatai.aiinteract.models.AiMessageType;
import com.chatai.aiinteract.models.AiResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP client for communicating with AI API endpoints.
 * Supports OpenAI-compatible chat completions API by default.
 */
public class AiApiClient {

    private final AiConfig config;
    private final Gson gson;
    private final ExecutorService executor;
    private final String systemPrompt;

    private static final String SYSTEM_PROMPT =
        "You are a helpful AI assistant in a chat application. " +
        "You can respond with text, voice, or video messages. " +
        "For voice responses, include a 'voice_url' field in your JSON response with a URL to an audio file. " +
        "For video responses, include a 'video_url' field in your JSON response with a URL to a video file.";

    public AiApiClient(AiConfig config) {
        this(config, SYSTEM_PROMPT);
    }

    public AiApiClient(AiConfig config, String systemPrompt) {
        this.config = config;
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
        this.systemPrompt = systemPrompt;
    }

    /**
     * Send a chat conversation to the AI API and get a response.
     *
     * @param conversationHistory Previous messages in the conversation
     * @param callback Callback for the response
     */
    public void sendMessage(List<AiMessage> conversationHistory, final AiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String requestBody = buildRequestBody(conversationHistory);
                    String responseBody = executeHttpRequest(requestBody);
                    AiResponse aiResponse = parseResponse(responseBody);
                    AiMessage result = convertToAiMessage(aiResponse);
                    callback.onResponse(result);
                } catch (IOException e) {
                    callback.onError(-1, "Network error: " + e.getMessage());
                } catch (Exception e) {
                    callback.onError(-2, "Error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Send a chat conversation with streaming response.
     */
    public void sendMessageStreaming(List<AiMessage> conversationHistory, final AiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String requestBody = buildRequestBodyStreaming(conversationHistory);
                    executeStreamingRequest(requestBody, callback);
                } catch (IOException e) {
                    callback.onError(-1, "Network error: " + e.getMessage());
                } catch (Exception e) {
                    callback.onError(-2, "Error: " + e.getMessage());
                }
            }
        });
    }

    private String buildRequestBody(List<AiMessage> conversationHistory) {
        Map<String, Object> body = new HashMap<>();

        // Add system message
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // Add conversation history
        for (AiMessage msg : conversationHistory) {
            messages.add(msg.toApiMap());
        }

        body.put("model", config.getModel());
        body.put("messages", messages);
        body.put("max_tokens", config.getMaxTokens());
        body.put("temperature", config.getTemperature());
        body.put("stream", false);

        return gson.toJson(body);
    }

    private String buildRequestBodyStreaming(List<AiMessage> conversationHistory) {
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        for (AiMessage msg : conversationHistory) {
            messages.add(msg.toApiMap());
        }

        body.put("model", config.getModel());
        body.put("messages", messages);
        body.put("max_tokens", config.getMaxTokens());
        body.put("temperature", config.getTemperature());
        body.put("stream", true);

        return gson.toJson(body);
    }

    private String executeHttpRequest(String requestBody) throws IOException {
        URL url = new URL(config.getApiEndpoint());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        // Write request body
        OutputStream os = connection.getOutputStream();
        os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        if (responseCode >= 400) {
            throw new IOException("API error " + responseCode + ": " + response.toString());
        }

        return response.toString();
    }

    private void executeStreamingRequest(String requestBody, final AiCallback callback) throws IOException {
        URL url = new URL(config.getApiEndpoint());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);

        OutputStream os = connection.getOutputStream();
        os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            InputStream errorStream = connection.getErrorStream();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
            StringBuilder errorBody = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorBody.append(line);
            }
            errorReader.close();
            connection.disconnect();
            throw new IOException("API error " + responseCode + ": " + errorBody.toString());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder fullContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    break;
                }
                try {
                    JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                    JsonArray choices = json.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject delta = choice.getAsJsonObject("delta");
                        if (delta != null && delta.has("content")) {
                            String chunk = delta.get("content").getAsString();
                            fullContent.append(chunk);
                            callback.onStreamChunk(chunk);
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed SSE lines
                }
            }
        }
        reader.close();
        connection.disconnect();

        callback.onStreamComplete();

        // Build final AiMessage from accumulated content
        AiMessage finalMessage = AiMessage.text(
                AiMessage.Role.ASSISTANT, fullContent.toString());
        callback.onResponse(finalMessage);
    }

    /**
     * Parse the AI API JSON response into an AiResponse object.
     * Supports OpenAI-compatible format with extensions for voice/video.
     */
    private AiResponse parseResponse(String responseBody) {
        AiResponse aiResponse = new AiResponse();
        aiResponse.setRawJson(responseBody);

        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // Check for top-level error
            if (json.has("error")) {
                JsonObject errorObj = json.getAsJsonObject("error");
                aiResponse.setError(errorObj.get("message").getAsString());
                return aiResponse;
            }

            // Parse choices from OpenAI-compatible format
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject messageObj = choice.getAsJsonObject("message");
                if (messageObj != null) {
                    // Text content
                    if (messageObj.has("content")) {
                        JsonElement contentEl = messageObj.get("content");
                        if (contentEl.isJsonPrimitive()) {
                            aiResponse.setTextContent(contentEl.getAsString());
                        } else if (contentEl.isJsonArray()) {
                            // Multimodal response - parse each part
                            parseMultimodalContent(contentEl.getAsJsonArray(), aiResponse);
                        }
                    }
                }
            }

            // Also check for direct voice/video URLs in response (custom API extension)
            if (json.has("voice_url")) {
                aiResponse.setVoiceUrl(json.get("voice_url").getAsString());
            }
            if (json.has("video_url")) {
                aiResponse.setVideoUrl(json.get("video_url").getAsString());
            }
            if (json.has("voice_duration")) {
                aiResponse.setVoiceDuration(json.get("voice_duration").getAsLong());
            }
            if (json.has("video_duration")) {
                aiResponse.setVideoDuration(json.get("video_duration").getAsLong());
            }

        } catch (Exception e) {
            // If JSON parsing fails, treat the raw response as text
            aiResponse.setTextContent(responseBody);
        }

        return aiResponse;
    }

    private void parseMultimodalContent(JsonArray contentArray, AiResponse response) {
        StringBuilder textBuilder = new StringBuilder();
        for (JsonElement element : contentArray) {
            JsonObject part = element.getAsJsonObject();
            String type = part.get("type").getAsString();
            switch (type) {
                case "text":
                    textBuilder.append(part.get("text").getAsString());
                    break;
                case "audio_url":
                case "input_audio":
                    JsonObject audioObj = part.getAsJsonObject("audio_url");
                    if (audioObj != null && audioObj.has("url")) {
                        response.setVoiceUrl(audioObj.get("url").getAsString());
                    }
                    break;
                case "video_url":
                    JsonObject videoObj = part.getAsJsonObject("video_url");
                    if (videoObj != null && videoObj.has("url")) {
                        response.setVideoUrl(videoObj.get("url").getAsString());
                    }
                    break;
            }
        }
        response.setTextContent(textBuilder.toString());
    }

    private AiMessage convertToAiMessage(AiResponse response) {
        if (response.hasError()) {
            return AiMessage.text(AiMessage.Role.ASSISTANT,
                    "Error: " + response.getError());
        }

        AiMessageType primaryType = response.getPrimaryType();

        switch (primaryType) {
            case VOICE: {
                AiMessage msg = AiMessage.voice(
                        AiMessage.Role.ASSISTANT,
                        response.getTextContent(),
                        response.getVoiceUrl());
                msg.setDuration(response.getVoiceDuration());
                return msg;
            }
            case VIDEO: {
                AiMessage msg = AiMessage.video(
                        AiMessage.Role.ASSISTANT,
                        response.getTextContent(),
                        response.getVideoUrl());
                msg.setDuration(response.getVideoDuration());
                return msg;
            }
            default:
                return AiMessage.text(AiMessage.Role.ASSISTANT,
                        response.getTextContent());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
