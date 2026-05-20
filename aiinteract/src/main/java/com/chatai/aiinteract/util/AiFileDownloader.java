package com.chatai.aiinteract.util;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads media files (voice audio, video) from remote URLs to local storage.
 */
public class AiFileDownloader {

    private final ExecutorService executor;

    public interface DownloadCallback {
        void onProgress(int progress);
        void onSuccess(String localFilePath);
        void onError(String error);
    }

    public AiFileDownloader() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Download a file from URL to the specified directory.
     * Generates a unique filename based on content type.
     *
     * @param url         The remote file URL
     * @param destDir     Local directory to save the file
     * @param fileSuffix  File extension (e.g., "mp3", "mp4")
     * @param callback    Download progress callback
     */
    public void download(String url, String destDir, String fileSuffix, final DownloadCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(destDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    String fileName = "ai_media_" + UUID.randomUUID().toString().substring(0, 8)
                            + "." + fileSuffix;
                    File outputFile = new File(dir, fileName);

                    URL downloadUrl = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(60000);
                    connection.connect();

                    int fileSize = connection.getContentLength();
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream outputStream = new FileOutputStream(outputFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    int lastProgress = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (fileSize > 0) {
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            if (progress != lastProgress) {
                                lastProgress = progress;
                                callback.onProgress(progress);
                            }
                        }
                    }

                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    connection.disconnect();

                    callback.onSuccess(outputFile.getAbsolutePath());

                } catch (IOException e) {
                    callback.onError("Download failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Get the default download directory for AI media files.
     */
    public static String getDefaultDownloadDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Chatai/AI_Media");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
