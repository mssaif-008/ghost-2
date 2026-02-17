package com.ghosthost.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Cloudflare R2 configuration.
 *
 * R2 is S3-compatible, so we use the AWS SDK S3 client.
 * The only trick is setting the endpoint to your R2 URL
 * instead of letting the SDK auto-detect an AWS region.
 *
 * HOW TO GET YOUR R2 CREDENTIALS:
 * 1. Go to Cloudflare Dashboard → R2
 * 2. Click "Manage R2 API Tokens"
 * 3. Create a token with "Object Read & Write" permission
 * 4. Copy the Access Key ID and Secret Access Key
 * 5. Your endpoint is: https://<account-id>.r2.cloudflarestorage.com
 *
 * COMMON MISTAKE: Forgetting to set the region. R2 doesn't care
 * about regions, but the AWS SDK requires one. Use "auto" or "us-east-1".
 */
@Configuration
public class R2Config {

    @Value("${r2.access-key}")
    private String accessKey;

    @Value("${r2.secret-key}")
    private String secretKey;

    @Value("${r2.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                // R2 doesn't use regions, but the SDK requires one
                .region(Region.of("auto"))
                .forcePathStyle(true)  // Required for S3-compatible services
                .build();
    }
}
