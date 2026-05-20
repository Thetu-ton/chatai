package com.chatai.memory.storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for conversation messages.
 *
 * <p>Provides queries for recent context retrieval, keyword search
 * (via FTS4 join), and memory management.
 */
@Dao
public interface StoredMessageDao {

    /**
     * Insert a single message. Returns the new row ID.
     * Auto-generated primary key ensures no conflicts.
     */
    @Insert
    long insert(StoredMessageEntity message);

    /**
     * Get the most recent N messages, ordered chronologically (oldest first).
     * Uses a subquery to pick the newest N, then sorts them ASC for LLM context.
     */
    @Query("SELECT * FROM (" +
           "  SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit" +
           ") ORDER BY timestamp ASC")
    List<StoredMessageEntity> getRecentMessages(int limit);

    /**
     * Search messages by keyword using FTS4.
     * Returns messages whose text matches the query, ordered by recency.
     */
    @Query("SELECT messages.* FROM messages " +
           "JOIN messages_fts ON messages.rowid = messages_fts.rowid " +
           "WHERE messages_fts MATCH :query " +
           "ORDER BY messages.timestamp DESC " +
           "LIMIT :limit")
    List<StoredMessageEntity> searchByKeyword(String query, int limit);

    /**
     * Search messages by keyword, but exclude the N most recent ones
     * (to avoid duplicating context already in the recent window).
     */
    @Query("SELECT messages.* FROM messages " +
           "JOIN messages_fts ON messages.rowid = messages_fts.rowid " +
           "WHERE messages_fts MATCH :query " +
           "AND messages.rowid NOT IN (" +
           "  SELECT rowid FROM messages ORDER BY timestamp DESC LIMIT :excludeRecent" +
           ") " +
           "ORDER BY messages.timestamp DESC " +
           "LIMIT :limit")
    List<StoredMessageEntity> searchExcludingRecent(String query, int excludeRecent, int limit);

    /**
     * Count total stored messages.
     */
    @Query("SELECT COUNT(*) FROM messages")
    int getCount();

    /**
     * Delete all messages.
     */
    @Query("DELETE FROM messages")
    void deleteAll();

    /**
     * Delete messages older than the given timestamp.
     */
    @Query("DELETE FROM messages WHERE timestamp < :before")
    void deleteOlderThan(long before);

    /**
     * Get all messages (for export/backup).
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<StoredMessageEntity> getAll();
}
