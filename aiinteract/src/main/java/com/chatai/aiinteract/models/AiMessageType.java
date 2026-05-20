package com.chatai.aiinteract.models;

/**
 * Types of AI-generated messages.
 */
public enum AiMessageType {
    TEXT(0),
    VOICE(1),
    VIDEO(2);

    private final int value;

    AiMessageType(int value) {
        this.value = value;
    }

    public int getValue() { return value; }

    public static AiMessageType fromValue(int value) {
        for (AiMessageType type : values()) {
            if (type.value == value) return type;
        }
        return TEXT;
    }

    public static AiMessageType fromString(String str) {
        if (str == null) return TEXT;
        String lower = str.toLowerCase();
        if (lower.contains("voice") || lower.contains("audio")) return VOICE;
        if (lower.contains("video")) return VIDEO;
        return TEXT;
    }
}
