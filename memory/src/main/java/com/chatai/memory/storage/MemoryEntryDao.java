package com.chatai.memory.storage;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for long-term memory entries.
 *
 * <p>Memory entries are durable facts extracted from conversations.
 * They survive indefinitely and are retrieved when relevant to the
 * current conversation context.
 */
@Dao
public interface MemoryEntryDao {

    /**
     * Insert or update a memory entry. If the memory_key already exists,
     * the value is updated and the timestamp refreshed.
     */
    @androidx.room.Upsert
    long upsert(MemoryEntryEntity entry);

    /**
     * Get a memory entry by its unique key.
     */
    @Query("SELECT * FROM memory_entries WHERE memory_key = :key LIMIT 1")
    MemoryEntryEntity getByKey(String key);

    /**
     * Search memories by keyword in summary and value fields.
     */
    @Query("SELECT * FROM memory_entries WHERE " +
           "memory_value LIKE '%' || :query || '%' OR " +
           "summary LIKE '%' || :query || '%' " +
           "ORDER BY importance DESC, access_count DESC " +
           "LIMIT :limit")
    List<MemoryEntryEntity> search(String query, int limit);

    /**
     * Get the most important memories for LLM context.
     * Combines importance score and recency.
     */
    @Query("SELECT * FROM memory_entries " +
           "ORDER BY importance DESC, updated_at DESC " +
           "LIMIT :limit")
    List<MemoryEntryEntity> getTopMemories(int limit);

    /**
     * Increment access count for a memory (called when it's retrieved).
     */
    @Query("UPDATE memory_entries SET access_count = access_count + 1, " +
           "updated_at = :now WHERE id = :id")
    void recordAccess(long id, long now);

    /**
     * Get all memory entries.
     */
    @Query("SELECT * FROM memory_entries ORDER BY updated_at DESC")
    List<MemoryEntryEntity> getAll();

    /**
     * Count memory entries.
     */
    @Query("SELECT COUNT(*) FROM memory_entries")
    int getCount();

    /**
     * Delete a memory by key.
     */
    @Query("DELETE FROM memory_entries WHERE memory_key = :key")
    void deleteByKey(String key);

    /**
     * Delete all memories.
     */
    @Query("DELETE FROM memory_entries")
    void deleteAll();
}
