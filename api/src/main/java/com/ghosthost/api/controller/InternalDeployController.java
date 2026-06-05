package com.ghosthost.api.controller;

import com.ghosthost.api.entity.BuildJob;
import com.ghosthost.api.entity.Deployment;
import com.ghosthost.api.repository.BuildJobRepository;
import com.ghosthost.api.repository.DeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Internal Deploy controller — handles callbacks from GitHub Actions.
 *
 * ENDPOINTS:
 * PATCH /api/internal/deployments/{id}/status — Update deployment status
 */
@RestController
@RequestMapping("/api/internal/deployments")
public class InternalDeployController {

    private static final Logger log = LoggerFactory.getLogger(InternalDeployController.class);

    private final DeploymentRepository deploymentRepository;
    private final BuildJobRepository buildJobRepository;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    public InternalDeployController(DeploymentRepository deploymentRepository,
                                    BuildJobRepository buildJobRepository) {
        this.deploymentRepository = deploymentRepository;
        this.buildJobRepository = buildJobRepository;
    }

    /**
     * Callback endpoint for GitHub Actions to update deployment status.
     *
     * Expected payload:
     * {
     *   "status": "LIVE",
     *   "error": "optional error message if status is FAILED"
     * }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateDeploymentStatus(
            @PathVariable("id") String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> payload) {

        // Validate API Key
        if (authHeader == null || !authHeader.replace("Bearer ", "").equals(internalApiKey)) {
            log.warn("Unauthorized callback attempt for deployment {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Deployment deployment = deploymentRepository.findById(id).orElse(null);
        if (deployment == null) {
            log.warn("Callback received for unknown deployment {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Deployment not found"));
        }

        String newStatus = payload.get("status");
        String errorMessage = payload.get("error");

        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        deployment.setStatus(newStatus);
        
        if ("FAILED".equals(newStatus)) {
            deployment.setErrorMessage(errorMessage != null ? errorMessage : "Build failed on GitHub Actions.");
            log.error("[{}] ❌ Deployment FAILED via callback: {}", id, deployment.getErrorMessage());
            
            logStep(id, "GITHUB_ACTIONS", "FAILED", deployment.getErrorMessage());
        } else if ("LIVE".equals(newStatus)) {
            log.info("[{}] 🎉 Deployment is LIVE via callback at: {}", id, deployment.getSiteUrl());
            logStep(id, "GITHUB_ACTIONS", "SUCCESS", "Build and upload completed successfully on GitHub Actions.");
        } else {
            log.info("[{}] Deployment status updated to {} via callback", id, newStatus);
        }

        deploymentRepository.save(deployment);

        return ResponseEntity.ok(Map.of("success", true));
    }

    private BuildJob logStep(String deploymentId, String step, String status, String message) {
        BuildJob job = new BuildJob(deploymentId, step, status);
        job.setLogOutput(message);
        job.setFinishedAt(LocalDateTime.now());
        return buildJobRepository.save(job);
    }
}
