package com.chatai.memory;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.commons.models.IUser;

/**
 * Converts between the UI message format (IMessage) and the AI message format (AiMessage).
 *
 * <p>This is the two-way bridge:
 * <ul>
 *   <li>IMessage (from UI) → AiMessage (to send to LLM)</li>
 *   <li>AiMessage (from LLM response) → IMessage (to display in UI)</li>
 * </ul>
 *
 * <p>Also creates synthetic IMessage instances for AI responses that need display.
 */
public final class MessageConverter {

    private MessageConverter() {}

    /**
     * Convert a UI message (IMessage) to an AI message (AiMessage) for sending to the LLM.
     * Determines the role (USER/ASSISTANT/SYSTEM) based on the message type.
     *
     * @param message    The IMessage from the UI
     * @param senderId   The local user's ID, used to determine if a message is from the user or AI
     * @return An AiMessage suitable for the LLM API
     */
    public static com.chatai.aiinteract.models.AiMessage toAiMessage(IMessage message, String senderId) {
        boolean isSent = isSentMessage(message.getType());
        boolean isFromUser = isSent || message.getFromUser().getId().equals(senderId);

        com.chatai.aiinteract.models.AiMessage.Role role =
            isFromUser ? com.chatai.aiinteract.models.AiMessage.Role.USER
                       : com.chatai.aiinteract.models.AiMessage.Role.ASSISTANT;

        switch (message.getType()) {
            case 1:  // SEND_TEXT
            case 0:  // RECEIVE_TEXT
                return com.chatai.aiinteract.models.AiMessage.text(role, message.getText());

            case 6:  // SEND_VOICE
            case 7:  // RECEIVE_VOICE:
                com.chatai.aiinteract.models.AiMessage voiceMsg =
                    com.chatai.aiinteract.models.AiMessage.voice(role,
                        message.getText() != null ? message.getText() : "[Voice message]",
                        message.getMediaFilePath());
                voiceMsg.setLocalMediaPath(message.getMediaFilePath());
                voiceMsg.setDuration(message.getDuration());
                return voiceMsg;

            case 8:  // SEND_VIDEO
            case 9:  // RECEIVE_VIDEO
                com.chatai.aiinteract.models.AiMessage videoMsg =
                    com.chatai.aiinteract.models.AiMessage.video(role,
                        message.getText() != null ? message.getText() : "[Video message]",
                        message.getMediaFilePath());
                videoMsg.setLocalMediaPath(message.getMediaFilePath());
                videoMsg.setDuration(message.getDuration());
                return videoMsg;

            case 2:  // SEND_IMAGE
            case 3:  // RECEIVE_IMAGE
                com.chatai.aiinteract.models.AiMessage imgMsg =
                    com.chatai.aiinteract.models.AiMessage.text(role,
                        message.getText() != null ? message.getText() : "[Image: " + message.getMediaFilePath() + "]");
                return imgMsg;

            default:
                return com.chatai.aiinteract.models.AiMessage.text(role,
                    message.getText() != null ? message.getText() : "");
        }
    }

    /**
     * Create an IMessage from an AiMessage for display in the UI.
     * Uses the same pattern as AiMessageBridge in the aiinteract module.
     *
     * @param aiMessage  The AI message (either user's or AI's response)
     * @param user       The IUser to display for this message
     * @param isReceived true if this is a received message (AI→User), false if sent (User→AI)
     */
    public static IMessage toDisplayMessage(com.chatai.aiinteract.models.AiMessage aiMessage,
                                            IUser user, boolean isReceived) {
        return new AiDisplayMessage(aiMessage, user, isReceived);
    }

    /**
     * Create a placeholder "typing..." IMessage for the AI.
     */
    public static IMessage createTypingIndicator(IUser aiUser) {
        return new AiDisplayMessage(
            com.chatai.aiinteract.models.AiMessage.text(
                com.chatai.aiinteract.models.AiMessage.Role.ASSISTANT, "..."),
            aiUser, true);
    }

    private static boolean isSentMessage(int type) {
        return type == 1 || type == 2 || type == 6 || type == 8 || type == 4 || type == 11;
    }

    /**
     * Internal IMessage implementation for AI-originated display messages.
     * Mirrors AiMessageBridge from aiinteract but lives inside the memory module.
     */
    private static class AiDisplayMessage implements IMessage {

        private final String msgId;
        private final String text;
        private final String timeString;
        private final int type;
        private final IUser user;
        private final String mediaFilePath;
        private final long duration;
        private MessageStatus status;

        AiDisplayMessage(com.chatai.aiinteract.models.AiMessage aiMessage, IUser fromUser, boolean isReceived) {
            this.msgId = UUID.randomUUID().toString();
            this.text = aiMessage.getContent();
            this.timeString = DateFormat.format("HH:mm", Calendar.getInstance(Locale.getDefault())).toString();
            this.user = fromUser;
            this.mediaFilePath = aiMessage.getLocalMediaPath();
            this.duration = aiMessage.getDuration();

            switch (aiMessage.getMessageType()) {
                case VOICE:
                    this.type = isReceived ? MessageType.RECEIVE_VOICE.ordinal() : MessageType.SEND_VOICE.ordinal();
                    break;
                case VIDEO:
                    this.type = isReceived ? MessageType.RECEIVE_VIDEO.ordinal() : MessageType.SEND_VIDEO.ordinal();
                    break;
                default:
                    this.type = isReceived ? MessageType.RECEIVE_TEXT.ordinal() : MessageType.SEND_TEXT.ordinal();
                    break;
            }

            this.status = isReceived ? MessageStatus.RECEIVE_SUCCEED : MessageStatus.SEND_SUCCEED;
        }

        @Override public String getMsgId() { return msgId; }
        @Override public IUser getFromUser() { return user; }
        @Override public String getTimeString() { return timeString; }
        @Override public int getType() { return type; }
        @Override public MessageStatus getMessageStatus() { return status; }
        @Override public String getText() { return text; }
        @Override public String getMediaFilePath() { return mediaFilePath; }
        @Override public long getDuration() { return duration; }
        @Override public String getProgress() { return ""; }
        @Override public HashMap<String, String> getExtras() { return null; }
    }
}
