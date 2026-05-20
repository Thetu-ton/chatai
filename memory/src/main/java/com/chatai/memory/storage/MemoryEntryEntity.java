package com.chatai.memory.storage;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A long-term memory entry — a fact, preference, or key piece of
 * information extracted from conversations and stored permanently.
 *
 * <p>Unlike raw conversation messages (which are transient context),
 * memory entries represent durable knowledge about the user that
 * persists across sessions and grows over time.
 *
 * <p>Examples:
 * <ul>
 *   <li>"User's name is Honor"</li>
 *   <li>"User prefers Python over Java"</li>
 *   <li>"User is working on Project X with deadline 2026-06-01"</li>
 *   <li>"User has a dog named Max"</li>
 * </ul>
 */
@Entity(tableName = "memory_entries", indices = {
    @Index(value = "memory_key", unique = true),
    @Index(value = "access_count"),
    @Index(value = "updated_at")
})
public class MemoryEntryEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * A unique key identifying this memory (e.g., "user_name", "pet_info").
     * Used to upsert — if the same key is written again, the value is updated.
     */
    @androidx.room.ColumnInfo(name = "memory_key")
    private String memoryKey;

    /** The memory content — a human-readable fact. */
    @androidx.room.ColumnInfo(name = "memory_value")
    private String memoryValue;

    /** Brief description for search/display purposes. */
    private String summary;

    /** 0-10 importance score. Higher = more likely to be included in LLM context. */
    private int importance;

    /** Number of times this memory has been retrieved (for ranking). */
    @androidx.room.ColumnInfo(name = "access_count")
    private int accessCount;

    /** When this memory was created (Unix ms). */
    @androidx.room.ColumnInfo(name = "created_at")
    private long createdAt;

    /** When this memory was last updated or accessed (Unix ms). */
    @androidx.room.ColumnInfo(name = "updated_at")
    private long updatedAt;

    /** ID of the conversation message that produced this memory (0 if manual). */
    @androidx.room.ColumnInfo(name = "source_message_id")
    private long sourceMessageId;

    public MemoryEntryEntity() {}

    @androidx.room.Ignore
    public MemoryEntryEntity(String memoryKey, String memoryValue, String summary,
                             int importance) {
        this.memoryKey = memoryKey;
        this.memoryValue = memoryValue;
        this.summary = summary;
        this.importance = importance;
        this.accessCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.sourceMessageId = 0;
    }

    // --- Getters / Setters ---

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMemoryKey() { return memoryKey; }
    public void setMemoryKey(String key) { this.memoryKey = key; }

    public String getMemoryValue() { return memoryValue; }
    public void setMemoryValue(String value) { this.memoryValue = value; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getImportance() { return importance; }
    public void setImportance(int importance) { this.importance = importance; }

    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int count) { this.accessCount = count; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(long id) { this.sourceMessageId = id; }
}
