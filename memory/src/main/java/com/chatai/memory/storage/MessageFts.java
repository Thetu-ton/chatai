package com.chatai.memory.storage;

import androidx.room.Entity;
import androidx.room.Fts4;

/**
 * FTS4-backed entity for full-text search over conversation messages.
 *
 * <p>FTS4 enables fast keyword search across all stored messages,
 * powering the "memory association" feature — when the user mentions
 * a topic, the memory module finds related past messages to include
 * as LLM context.
 *
 * <p>This is a virtual FTS table that mirrors the content of
 * {@link StoredMessageEntity} for search purposes.
 */
@Fts4(contentEntity = StoredMessageEntity.class)
@Entity(tableName = "messages_fts")
public class MessageFts {

    /**
     * The rowid of the corresponding StoredMessageEntity.
     * FTS4 with contentEntity sync uses this implicitly.
     */
    @androidx.room.ColumnInfo(name = "rowid")
    private long rowid;

    /** Text content for full-text indexing. */
    @androidx.room.ColumnInfo(name = "text")
    private String text;

    /** Sender identifier for filtering. */
    @androidx.room.ColumnInfo(name = "sender_id")
    private String senderId;

    public MessageFts(long rowid, String text, String senderId) {
        this.rowid = rowid;
        this.text = text;
        this.senderId = senderId;
    }

    public long getRowid() { return rowid; }
    public void setRowid(long rowid) { this.rowid = rowid; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
}
