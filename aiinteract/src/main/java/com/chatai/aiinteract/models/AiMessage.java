package com.chatai.aiinteract.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single message in the AI chat conversation.
 * Can be text, voice, or video.
 */
public class AiMessage {

    public enum Role {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system");

        private final String value;
        Role(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private Role role;
    private String content;
    private AiMessageType messageType;
    private String mediaUrl;
    private String localMediaPath;
    private long duration;
    private Map<String, Object> extras;

    private AiMessage(Role role, String content, AiMessageType type) {
        this.role = role;
        this.content = content;
        this.messageType = type;
        this.extras = new HashMap<>();
    }

    // --- Factory methods ---

    public static AiMessage text(Role role, String text) {
        return new AiMessage(role, text, AiMessageType.TEXT);
    }

    public static AiMessage voice(Role role, String text, String mediaUrl) {
        AiMessage msg = new AiMessage(role, text, AiMessageType.VOICE);
        msg.mediaUrl = mediaUrl;
        return msg;
    }

    public static AiMessage video(Role role, String text, String mediaUrl) {
        AiMessage msg = new AiMessage(role, text, AiMessageType.VIDEO);
        msg.mediaUrl = mediaUrl;
        return msg;
    }

    // --- Getters / Setters ---

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AiMessageType getMessageType() { return messageType; }
    public void setMessageType(AiMessageType type) { this.messageType = type; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String url) { this.mediaUrl = url; }

    public String getLocalMediaPath() { return localMediaPath; }
    public void setLocalMediaPath(String path) { this.localMediaPath = path; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public Map<String, Object> getExtras() { return extras; }
    public void setExtras(Map<String, Object> extras) { this.extras = extras; }

    /**
     * Convert to a JSON-serializable map for the AI API request body.
     */
    public Map<String, Object> toApiMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("role", role.getValue());

        if (messageType == AiMessageType.TEXT) {
            map.put("content", content);
        } else {
            // For multimodal messages, send as structured content
            List<Map<String, Object>> contentParts = new java.util.ArrayList<>();
            if (content != null && !content.isEmpty()) {
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", content);
                contentParts.add(textPart);
            }
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                Map<String, Object> mediaPart = new HashMap<>();
                if (messageType == AiMessageType.VOICE) {
                    mediaPart.put("type", "audio_url");
                    Map<String, String> audioDetail = new HashMap<>();
                    audioDetail.put("url", mediaUrl);
                    mediaPart.put("audio_url", audioDetail);
                } else if (messageType == AiMessageType.VIDEO) {
                    mediaPart.put("type", "video_url");
                    Map<String, String> videoDetail = new HashMap<>();
                    videoDetail.put("url", mediaUrl);
                    mediaPart.put("video_url", videoDetail);
                }
                contentParts.add(mediaPart);
            }
            map.put("content", contentParts);
        }
        return map;
    }

    @Override
    public String toString() {
        return "AiMessage{role=" + role + ", type=" + messageType + ", content=" + content + "}";
    }
}
