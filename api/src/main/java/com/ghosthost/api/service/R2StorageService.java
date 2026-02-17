package com.ghosthost.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * R2 Storage Service — uploads build artifacts to Cloudflare R2.
 *
 * R2 is Cloudflare's S3-compatible object storage.
 * We use the AWS SDK S3 client (configured in R2Config) to upload files.
 *
 * FILE PATH STRUCTURE IN R2:
 * deployments/{deploymentId}/index.html
 * deployments/{deploymentId}/assets/style.css
 * deployments/{deploymentId}/assets/app.js
 *
 * This structure allows Nginx to map:
 * {deploymentId}.mydomain.com/assets/style.css
 * → R2: deployments/{deploymentId}/assets/style.css
 *
 * HOW TO SET UP R2:
 * 1. Go to Cloudflare Dashboard → R2
 * 2. Create a bucket (e.g., "ghosthost-deployments")
 * 3. Enable public access on the bucket
 * 4. Create an R2 API token with read/write access
 * 5. Set the credentials in .env
 */
@Service
public class R2StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private final S3Client s3Client;

    @Value("${r2.bucket}")
    private String bucketName;

    public R2StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Upload all files from a local directory to R2.
     *
     * @param localDir     The local directory containing built files
     * @param deploymentId The deployment ID (used as the R2 prefix)
     * @return Number of files uploaded
     */
    public int uploadDirectory(Path localDir, String deploymentId) throws IOException {
        int count = 0;

        // Walk the directory tree and upload each file
        try (Stream<Path> paths = Files.walk(localDir)) {
            for (Path filePath : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                // Calculate the relative path for R2
                // e.g., /tmp/abc123/dist/index.html → deployments/abc123/index.html
                String relativePath = localDir.relativize(filePath).toString()
                        .replace("\\", "/"); // Windows path fix

                String r2Key = "deployments/" + deploymentId + "/" + relativePath;

                // Detect content type
                String contentType = URLConnection.guessContentTypeFromName(filePath.toString());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // Upload to R2
                log.info("Uploading: {} → r2://{}/{}", relativePath, bucketName, r2Key);

                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(r2Key)
                        .contentType(contentType)
                        .build();

                s3Client.putObject(putRequest,
                        RequestBody.fromFile(filePath.toFile()));

                count++;
            }
        }

        log.info("Uploaded {} files for deployment {}", count, deploymentId);
        return count;
    }
}
