package com.chatai.memory.mem0;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP client for the Mem0 REST API.
 *
 * <p>Mem0 is an open-source memory layer that provides:
 * <ul>
 *   <li>Automatic memory extraction from conversations</li>
 *   <li>Vector-based semantic search</li>
 *   <li>Memory deduplication and merging</li>
 * </ul>
 *
 * <p>API endpoints used:
 * <ul>
 *   <li>{@code POST /v1/memories/} — add memories</li>
 *   <li>{@code POST /v1/memories/search/} — semantic search</li>
 *   <li>{@code GET /v1/memories/?user_id=...} — get all memories</li>
 *   <li>{@code DELETE /v1/memories/{id}} — delete a memory</li>
 * </ul>
 */
public class Mem0Client {

    private final String baseUrl;
    private final String apiKey;
    private final String userId;
    private final Gson gson;
    private final ExecutorService executor;

    /**
     * @param baseUrl Mem0 server URL (e.g., "https://api.mem0.ai" or "http://localhost:8000")
     * @param apiKey  API key for authentication
     * @param userId  The user ID to scope memories to
     */
    public Mem0Client(String baseUrl, String apiKey, String userId) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.userId = userId;
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
    }

    // ==================== Public API ====================

    /**
     * Callback for async Mem0 operations.
     */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(int code, String message);
    }

    /**
     * Add a message to Mem0. Mem0 automatically extracts and consolidates memories.
     *
     * @param content  The message text to extract memories from
     * @param metadata Optional metadata (importance, source, etc.)
     * @param callback Called with the list of created/updated memories
     */
    public void addMemory(String content, Map<String, Object> metadata, Callback<List<Mem0Models.MemoryItem>> callback) {
        executor.execute(() -> {
            try {
                List<Mem0Models.Message> messages = new ArrayList<>();
                messages.add(new Mem0Models.Message("user", content));

                Mem0Models.AddMemoryRequest request = new Mem0Models.AddMemoryRequest(
                    messages, userId, metadata
                );

                String body = gson.toJson(request);
                String response = httpPost("/v1/memories/", body);

                Mem0Models.AddMemoryResponse parsed = gson.fromJson(
                    response, Mem0Models.AddMemoryResponse.class);

                if (callback != null) {
                    callback.onSuccess(parsed.memories != null ? parsed.memories : new ArrayList<>());
                }
            } catch (IOException e) {
                if (callback != null) {
                    callback.onError(-1, "Mem0 addMemory failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Search memories by semantic similarity.
     *
     * @param query    Natural language query
     * @param limit    Max results
     * @param callback Called with matching memories sorted by relevance
     */
    public void searchMemories(String query, int limit, Callback<List<Mem0Models.MemoryItem>> callback) {
        executor.execute(() -> {
            try {
                Mem0Models.SearchMemoryRequest request = new Mem0Models.SearchMemoryRequest(
                    query, userId, limit
                );

                String body = gson.toJson(request);
                String response = httpPost("/v1/memories/search/", body);

                Mem0Models.SearchMemoryResponse parsed = gson.fromJson(
                    response, Mem0Models.SearchMemoryResponse.class);

                if (callback != null) {
                    callback.onSuccess(parsed.results != null ? parsed.results : new ArrayList<>());
                }
            } catch (IOException e) {
                if (callback != null) {
                    callback.onError(-1, "Mem0 search failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Get all memories for the configured user.
     */
    public void getAllMemories(Callback<List<Mem0Models.MemoryItem>> callback) {
        executor.execute(() -> {
            try {
                String response = httpGet("/v1/memories/?user_id=" + urlEncode(userId));

                Mem0Models.GetAllMemoriesResponse parsed = gson.fromJson(
                    response, Mem0Models.GetAllMemoriesResponse.class);

                if (callback != null) {
                    callback.onSuccess(parsed.memories != null ? parsed.memories : new ArrayList<>());
                }
            } catch (IOException e) {
                if (callback != null) {
                    callback.onError(-1, "Mem0 getAll failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Delete a memory by ID.
     */
    public void deleteMemory(String memoryId, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                httpDelete("/v1/memories/" + memoryId);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } catch (IOException e) {
                if (callback != null) {
                    callback.onError(-1, "Mem0 delete failed: " + e.getMessage());
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    // ==================== HTTP Helpers ====================

    private String httpPost(String path, String body) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Token " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        return readResponse(conn);
    }

    private String httpGet(String path) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Token " + apiKey);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        return readResponse(conn);
    }

    private void httpDelete(String path) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Token " + apiKey);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int code = conn.getResponseCode();
        if (code >= 400) {
            throw new IOException("HTTP " + code + ": " + readErrorStream(conn));
        }
        conn.disconnect();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        if (code >= 400) {
            throw new IOException("HTTP " + code + ": " + sb.toString());
        }
        return sb.toString();
    }

    private String readErrorStream(HttpURLConnection conn) throws IOException {
        InputStream es = conn.getErrorStream();
        if (es == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
