package com.itr.infrastructure.storage;

/**
 * DocumentStorageProvider — interface for document/file storage (S3/MinIO/local).
 */
public interface DocumentStorageProvider {

    /**
     * Upload a file.
     *
     * @param key        Storage key (path)
     * @param data       File bytes
     * @param mimeType   MIME type
     * @return Storage URL or key
     */
    String upload(String key, byte[] data, String mimeType);

    /**
     * Download a file.
     *
     * @param key Storage key
     * @return File bytes
     */
    byte[] download(String key);

    /**
     * Delete a file.
     *
     * @param key Storage key
     */
    void delete(String key);

    /**
     * Generate a presigned URL for secure temporary access.
     *
     * @param key          Storage key
     * @param expiryMinutes Expiry in minutes
     * @return Presigned URL
     */
    String generatePresignedUrl(String key, int expiryMinutes);
}
