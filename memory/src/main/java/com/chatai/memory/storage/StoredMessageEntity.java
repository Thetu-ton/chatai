package com.chatai.memory.storage;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for a single stored chat message.
 *
 * <p>This is the canonical message store. A companion FTS4 virtual table
 * ({@link MessageFts}) mirrors the text column for full-text search.
 *
 * <p>Messages survive app restarts and provide the "long-term memory"
 * foundation — every conversation is preserved on disk.
 */
@Entity(tableName = "messages", indices = {
    @Index(value = "timestamp"),
    @Index(value = "sender_id")
})
public class StoredMessageEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** "user" or "ai" — determines the AiMessage.Role on retrieval. */
    @androidx.room.ColumnInfo(name = "sender_id")
    private String senderId;

    /** The message text content. */
    private String text;

    /** Local file path for media messages (voice/video). */
    @androidx.room.ColumnInfo(name = "media_file_path")
    private String mediaFilePath;

    /** Remote URL for AI-generated media. */
    @androidx.room.ColumnInfo(name = "media_url")
    private String mediaUrl;

    /** Duration in seconds for voice/video messages. */
    private long duration;

    /**
     * Message type ordinal from IMessage.MessageType:
     * 0=RECEIVE_TEXT, 1=SEND_TEXT, 6=SEND_VOICE, 7=RECEIVE_VOICE,
     * 8=SEND_VIDEO, 9=RECEIVE_VIDEO
     */
    private int type;

    /** Unix timestamp in milliseconds (when the message was created). */
    private long timestamp;

    public StoredMessageEntity() {}

    @androidx.room.Ignore
    public StoredMessageEntity(String senderId, String text, int type, String mediaFilePath,
                               String mediaUrl, long duration, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.mediaFilePath = mediaFilePath;
        this.mediaUrl = mediaUrl;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    // --- Getters / Setters (required by Room) ---

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getMediaFilePath() { return mediaFilePath; }
    public void setMediaFilePath(String path) { this.mediaFilePath = path; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String url) { this.mediaUrl = url; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Convert to an AiMessage for the AI interaction layer.
     */
    public com.chatai.aiinteract.models.AiMessage toAiMessage() {
        com.chatai.aiinteract.models.AiMessage.Role role =
            "user".equals(senderId)
                ? com.chatai.aiinteract.models.AiMessage.Role.USER
                : com.chatai.aiinteract.models.AiMessage.Role.ASSISTANT;

        boolean isVoice = type == 6 || type == 7;
        boolean isVideo = type == 8 || type == 9;

        com.chatai.aiinteract.models.AiMessage msg;
        if (isVoice) {
            msg = com.chatai.aiinteract.models.AiMessage.voice(role, text, mediaUrl);
        } else if (isVideo) {
            msg = com.chatai.aiinteract.models.AiMessage.video(role, text, mediaUrl);
        } else {
            msg = com.chatai.aiinteract.models.AiMessage.text(role, text);
        }
        msg.setLocalMediaPath(mediaFilePath);
        msg.setDuration(duration);
        return msg;
    }
}
