package com.chatai.aiinteract.models;

/**
 * Parsed response from the AI API.
 * Contains the response content and metadata about message types.
 */
public class AiResponse {

    private String textContent;
    private String voiceUrl;
    private String videoUrl;
    private long voiceDuration;
    private long videoDuration;
    private String error;
    private String rawJson;

    public AiResponse() {}

    public String getTextContent() { return textContent; }
    public void setTextContent(String text) { this.textContent = text; }

    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String url) { this.voiceUrl = url; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String url) { this.videoUrl = url; }

    public long getVoiceDuration() { return voiceDuration; }
    public void setVoiceDuration(long duration) { this.voiceDuration = duration; }

    public long getVideoDuration() { return videoDuration; }
    public void setVideoDuration(long duration) { this.videoDuration = duration; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    public boolean hasError() { return error != null && !error.isEmpty(); }

    /**
     * Determine the primary message type from the response.
     */
    public AiMessageType getPrimaryType() {
        if (videoUrl != null && !videoUrl.isEmpty()) return AiMessageType.VIDEO;
        if (voiceUrl != null && !voiceUrl.isEmpty()) return AiMessageType.VOICE;
        return AiMessageType.TEXT;
    }

    /**
     * Check if response has text content alongside media.
     */
    public boolean hasTextContent() {
        return textContent != null && !textContent.isEmpty();
    }

    @Override
    public String toString() {
        return "AiResponse{text=" + textContent + ", voice=" + voiceUrl + ", video=" + videoUrl + "}";
    }
}
