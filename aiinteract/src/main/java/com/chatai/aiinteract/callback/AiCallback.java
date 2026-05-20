package com.chatai.aiinteract.callback;

import com.chatai.aiinteract.models.AiMessage;

/**
 * Callback interface for AI interaction results.
 * The UI layer implements this to receive AI responses.
 */
public interface AiCallback {

    /**
     * Called when a complete AI response is received.
     * @param message The AI's response message (text, voice, or video)
     */
    void onResponse(AiMessage message);

    /**
     * Called for each chunk during streaming text responses.
     * @param chunk Partial text from the AI
     */
    void onStreamChunk(String chunk);

    /**
     * Called when streaming is complete.
     */
    void onStreamComplete();

    /**
     * Called when an error occurs.
     * @param code Error code for programmatic handling
     * @param message Human-readable error message
     */
    void onError(int code, String message);

    /**
     * Called during file download (voice/video media from AI).
     * @param progress Percentage 0-100
     */
    void onFileDownloadProgress(int progress);
}
