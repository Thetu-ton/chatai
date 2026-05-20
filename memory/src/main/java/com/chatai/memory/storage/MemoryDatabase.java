package com.chatai.memory.storage;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database holding all persistent memory data.
 *
 * <p>Contains two stores:
 * <ul>
 *   <li><b>messages</b> — raw conversation history (short-term context)</li>
 *   <li><b>memory_entries</b> — extracted long-term facts (permanent memory)</li>
 *   <li><b>messages_fts</b> — FTS4 virtual table for full-text search</li>
 * </ul>
 *
 * <p>Singleton pattern — one database instance per process.
 */
@Database(
    entities = {
        StoredMessageEntity.class,
        MemoryEntryEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class MemoryDatabase extends RoomDatabase {

    private static final String DB_NAME = "chatai_memory.db";

    private static volatile MemoryDatabase instance;

    public abstract StoredMessageDao messageDao();
    public abstract MemoryEntryDao memoryEntryDao();

    /**
     * Get (or create) the singleton database instance.
     */
    public static MemoryDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (MemoryDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        MemoryDatabase.class,
                        DB_NAME
                    )
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return instance;
    }

    /**
     * Explicitly close and release the database.
     */
    public static void destroyInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
