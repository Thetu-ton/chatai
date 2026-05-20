package com.chatai.memory.mem0;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Data models for the Mem0 REST API.
 *
 * <p>Mem0 is an open-source memory layer for AI apps.
 * API reference: https://docs.mem0.ai/api-reference
 */
public final class Mem0Models {

    private Mem0Models() {}

    // ==================== Request Models ====================

    /** Request body for POST /v1/memories/ — add memories. */
    public static class AddMemoryRequest {
        /** Conversation messages to extract memories from. */
        public List<Message> messages;

        /** Unique user identifier. */
        @SerializedName("user_id")
        public String userId;

        /** Optional metadata (e.g., {"source": "chat", "importance": 8}). */
        public Map<String, Object> metadata;

        public AddMemoryRequest(List<Message> messages, String userId, Map<String, Object> metadata) {
            this.messages = messages;
            this.userId = userId;
            this.metadata = metadata;
        }
    }

    /** Request body for POST /v1/memories/search/ — search memories. */
    public static class SearchMemoryRequest {
        /** Natural language query for semantic search. */
        public String query;

        /** Unique user identifier. */
        @SerializedName("user_id")
        public String userId;

        /** Max results to return (default 10). */
        public int limit;

        public SearchMemoryRequest(String query, String userId, int limit) {
            this.query = query;
            this.userId = userId;
            this.limit = limit;
        }
    }

    /** A single message in the add-memory request. */
    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    // ==================== Response Models ====================

    /** Response from POST /v1/memories/ — list of created/updated memories. */
    public static class AddMemoryResponse {
        public List<MemoryItem> memories;
    }

    /** Response from POST /v1/memories/search/ — search results. */
    public static class SearchMemoryResponse {
        public List<MemoryItem> results;
    }

    /** Response from GET /v1/memories/ — all memories for a user. */
    public static class GetAllMemoriesResponse {
        public List<MemoryItem> memories;
    }

    /** A single memory item returned by Mem0. */
    public static class MemoryItem {
        /** Unique memory ID. */
        public String id;

        /** The memory text content. */
        public String memory;

        /** User ID this memory belongs to. */
        @SerializedName("user_id")
        public String userId;

        /** Creation timestamp. */
        @SerializedName("created_at")
        public String createdAt;

        /** Last update timestamp. */
        @SerializedName("updated_at")
        public String updatedAt;

        /** Optional metadata map. */
        public Map<String, Object> metadata;

        /** Similarity score (only in search results). */
        public Float score;
    }
}
