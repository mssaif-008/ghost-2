package com.ghosthost.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Supabase Storage Service — uploads build artifacts to Supabase Storage.
 *
 * Uses the Supabase Storage REST API to upload files.
 *
 * FILE PATH STRUCTURE IN SUPABASE STORAGE:
 * deployments/{deploymentId}/index.html
 * deployments/{deploymentId}/assets/style.css
 * deployments/{deploymentId}/assets/app.js
 *
 * This structure allows Nginx to map:
 * {deploymentId}.mydomain.com/assets/style.css
 * → Supabase:
 * {url}/storage/v1/object/public/{bucket}/deployments/{deploymentId}/assets/style.css
 *
 * HOW TO SET UP SUPABASE STORAGE:
 * 1. Go to Supabase Dashboard → Storage
 * 2. Create a bucket (e.g., "ghosthost-deployments")
 * 3. Set the bucket to public (for serving static files)
 * 4. Copy the project URL and service_role key from Settings → API
 * 5. Set the credentials in .env
 *
 * API ENDPOINT:
 * POST {supabase_url}/storage/v1/object/{bucket}/{path}
 * Headers: Authorization: Bearer {service_role_key}
 * Content-Type: {detected mime type}
 * x-upsert: true (overwrite if exists)
 */
@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    public SupabaseStorageService(RestTemplate supabaseRestTemplate) {
        this.restTemplate = supabaseRestTemplate;
    }

    /**
     * Upload all files from a local directory to Supabase Storage.
     *
     * @param localDir     The local directory containing built files
     * @param deploymentId The deployment ID (used as the storage prefix)
     * @return Number of files uploaded
     */
    public int uploadDirectory(Path localDir, String deploymentId) throws IOException {
        int count = 0;

        // Walk the directory tree and upload each file
        try (Stream<Path> paths = Files.walk(localDir)) {
            for (Path filePath : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                // Calculate the relative path for storage
                // e.g., /tmp/abc123/dist/index.html → deployments/abc123/index.html
                String relativePath = localDir.relativize(filePath).toString()
                        .replace("\\", "/"); // Windows path fix

                String storagePath = "deployments/" + deploymentId + "/" + relativePath;

                // Detect content type
                String contentType = URLConnection.guessContentTypeFromName(filePath.toString());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // Upload to Supabase Storage
                log.info("Uploading: {} → supabase://{}/{}", relativePath, bucketName, storagePath);

                uploadFile(filePath, storagePath, contentType);
                count++;
            }
        }

        log.info("Uploaded {} files for deployment {}", count, deploymentId);
        return count;
    }

    /**
     * Upload a single file to Supabase Storage.
     *
     * Uses the Supabase Storage REST API:
     * POST {supabase_url}/storage/v1/object/{bucket}/{path}
     */
    private void uploadFile(Path filePath, String storagePath, String contentType) throws IOException {
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + storagePath;

        byte[] fileBytes = Files.readAllBytes(filePath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-upsert", "true"); // Overwrite if file already exists

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Failed to upload " + storagePath
                    + ": HTTP " + response.getStatusCode()
                    + " — " + response.getBody());
        }
    }
}
