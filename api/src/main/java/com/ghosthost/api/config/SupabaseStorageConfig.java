package com.ghosthost.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Supabase Storage configuration.
 *
 * Supabase Storage exposes a REST API for uploading and managing files.
 * We use Spring's RestTemplate to interact with it.
 *
 * HOW TO GET YOUR SUPABASE CREDENTIALS:
 * 1. Go to Supabase Dashboard → select your project
 * 2. Go to Settings → API
 * 3. Copy the Project URL (e.g., https://xyz.supabase.co)
 * 4. Copy the "service_role" key (NOT the anon key — we need write access)
 * 5. Go to Storage → Create a new bucket (e.g., "ghosthost-deployments")
 * 6. Make the bucket public if you want files served without auth
 *
 * IMPORTANT: The service_role key bypasses RLS. Keep it secret.
 * Only use it server-side, never expose it to the frontend.
 */
@Configuration
public class SupabaseStorageConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Bean
    public RestTemplate supabaseRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10 seconds
        factory.setReadTimeout(60_000); // 60 seconds (large file uploads)
        return new RestTemplate(factory);
    }
}
