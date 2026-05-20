package com.chatai.aiinteract.bridge;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.commons.models.IUser;

/**
 * Bridge class that adapts AiMessage to the IMUI IMessage interface,
 * so AI responses can be rendered directly by the existing MsgListAdapter.
 *
 * <p>The UI team uses this to create displayable messages from AI responses.
 *
 * <p>Usage:
 * <pre>
 *   AiInteract.getInstance().sendTextMessage("Hello", new AiCallback() {
 *       public void onResponse(AiMessage aiMessage) {
 *           AiMessageBridge bridge = new AiMessageBridge(aiMessage, aiUser, isReceived);
 *           mAdapter.addToStart(bridge, true);
 *       }
 *   });
 * </pre>
 */
public class AiMessageBridge implements IMessage {

    private final long id;
    private final String text;
    private final String timeString;
    private final int type;
    private final IUser user;
    private final String mediaFilePath;
    private final long duration;
    private final String progress;
    private MessageStatus status;

    /**
     * Create a bridge from an AiMessage.
     *
     * @param aiMessage The AI message to wrap
     * @param fromUser  The user who sent/received this message (for display)
     * @param isReceived true if this is a received message (from AI), false if from the local user
     */
    public AiMessageBridge(com.chatai.aiinteract.models.AiMessage aiMessage, IUser fromUser, boolean isReceived) {
        this.id = UUID.randomUUID().getLeastSignificantBits();
        this.text = aiMessage.getContent();
        this.timeString = DateFormat.format("HH:mm", Calendar.getInstance(Locale.getDefault())).toString();
        this.user = fromUser;
        this.mediaFilePath = aiMessage.getLocalMediaPath();
        this.duration = aiMessage.getDuration();

        // Map AiMessageType to IMessage.MessageType
        switch (aiMessage.getMessageType()) {
            case VOICE:
                this.type = isReceived ? MessageType.RECEIVE_VOICE.ordinal() : MessageType.SEND_VOICE.ordinal();
                break;
            case VIDEO:
                this.type = isReceived ? MessageType.RECEIVE_VIDEO.ordinal() : MessageType.SEND_VIDEO.ordinal();
                break;
            case TEXT:
            default:
                this.type = isReceived ? MessageType.RECEIVE_TEXT.ordinal() : MessageType.SEND_TEXT.ordinal();
                break;
        }

        this.status = isReceived ? MessageStatus.RECEIVE_SUCCEED : MessageStatus.SEND_SUCCEED;
        this.progress = "";
    }

    /**
     * Create a bridge for a sending (user-originated) message.
     */
    public static AiMessageBridge forSending(com.chatai.aiinteract.models.AiMessage aiMessage, IUser user) {
        AiMessageBridge bridge = new AiMessageBridge(aiMessage, user, false);
        bridge.status = MessageStatus.SEND_GOING;
        return bridge;
    }

    /**
     * Create a bridge for a received (AI-originated) message.
     */
    public static AiMessageBridge forReceived(com.chatai.aiinteract.models.AiMessage aiMessage, IUser aiUser) {
        return new AiMessageBridge(aiMessage, aiUser, true);
    }

    /**
     * Update status to indicate send success/failure.
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    @Override
    public String getMsgId() {
        return String.valueOf(id);
    }

    @Override
    public IUser getFromUser() {
        return user;
    }

    @Override
    public String getTimeString() {
        return timeString;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public MessageStatus getMessageStatus() {
        return status;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getMediaFilePath() {
        return mediaFilePath;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String getProgress() {
        return progress;
    }

    @Override
    public HashMap<String, String> getExtras() {
        return null;
    }
}
