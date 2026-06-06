package com.ghosthost.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class SiteController {

    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    public SiteController(RestTemplate supabaseRestTemplate) {
        this.restTemplate = supabaseRestTemplate;
    }

    @GetMapping({"/sites/{deploymentId}", "/sites/{deploymentId}/", "/sites/{deploymentId}/**"})
    public ResponseEntity<byte[]> serveSiteFile(
            @PathVariable String deploymentId,
            HttpServletRequest request) {

        if (!deploymentId.matches("[a-zA-Z0-9-]+")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new byte[0]);
        }

        String requestedPath = extractSitePath(request.getRequestURI(), deploymentId);
        if (requestedPath.isBlank()) {
            requestedPath = "index.html";
        }

        ResponseEntity<byte[]> response = fetchStorageObject(deploymentId, requestedPath);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        if (!"index.html".equals(requestedPath)) {
            ResponseEntity<byte[]> fallback = fetchStorageObject(deploymentId, "index.html");
            if (fallback.getStatusCode().is2xxSuccessful()) {
                return fallback;
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]);
    }

    private String extractSitePath(String requestUri, String deploymentId) {
        String prefix = "/sites/" + deploymentId;
        String path = requestUri.startsWith(prefix) ? requestUri.substring(prefix.length()) : "";
        path = path.replaceFirst("^/+", "");

        if (path.contains("..") || path.startsWith("/")) {
            return "index.html";
        }
        return path;
    }

    private ResponseEntity<byte[]> fetchStorageObject(String deploymentId, String relativePath) {
        String storagePath = "deployments/" + deploymentId + "/" + relativePath;
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + encodePath(storagePath);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setBearerAuth(serviceKey);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(requestHeaders),
                    byte[].class);

            byte[] body = response.getBody() != null ? response.getBody() : new byte[0];
            if ("index.html".equals(relativePath)) {
                body = rewriteIndexHtml(deploymentId, body);
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(contentTypeForPath(relativePath));
            responseHeaders.setCacheControl("public, max-age=60");

            return new ResponseEntity<>(
                    body,
                    responseHeaders,
                    response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new byte[0]);
        }
    }

    private byte[] rewriteIndexHtml(String deploymentId, byte[] body) {
        String html = new String(body, StandardCharsets.UTF_8);
        String prefix = "/sites/" + deploymentId + "/";
        html = html.replace("href=\"/", "href=\"" + prefix)
                .replace("src=\"/", "src=\"" + prefix)
                .replace("action=\"/", "action=\"" + prefix)
                .replace("url(/", "url(" + prefix);
        return html.getBytes(StandardCharsets.UTF_8);
    }

    private MediaType contentTypeForPath(String path) {
        if (path.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        }
        if (path.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        }
        if (path.endsWith(".js") || path.endsWith(".mjs")) {
            return MediaType.valueOf("text/javascript");
        }
        if (path.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        }
        String contentType = URLConnection.guessContentTypeFromName(path);
        return contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
    }

    private String encodePath(String path) {
        String[] segments = path.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append("/");
            }
            encoded.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8)
                    .replace("+", "%20"));
        }
        return encoded.toString();
    }
}
