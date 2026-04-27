package com.ghosthost.api.controller;

import com.ghosthost.api.dto.DeployRequest;
import com.ghosthost.api.dto.DeployResponse;
import com.ghosthost.api.entity.BuildJob;
import com.ghosthost.api.repository.BuildJobRepository;
import com.ghosthost.api.repository.DeploymentRepository;
import com.ghosthost.api.service.DeployService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Deploy controller — handles deployment CRUD and logs.
 *
 * ENDPOINTS:
 * POST /deploy — create a new deployment (requires JWT)
 * GET /deploy/{id} — get deployment status
 * GET /logs/{id} — get build logs for a deployment
 *
 * HOW AUTH WORKS HERE:
 * The JwtAuthFilter sets the userId as the "principal" in SecurityContext.
 * We extract it via Authentication.getPrincipal() in each method.
 */
@RestController
public class DeployController {

    private final DeployService deployService;
    private final BuildJobRepository buildJobRepository;
    private final DeploymentRepository deploymentRepository;

    public DeployController(DeployService deployService,
            BuildJobRepository buildJobRepository,
            DeploymentRepository deploymentRepository) {
        this.deployService = deployService;
        this.buildJobRepository = buildJobRepository;
        this.deploymentRepository = deploymentRepository;
    }

    /**
     * Create a new deployment.
     *
     * curl -X POST http://localhost:8080/deploy \
     * -H "Authorization: Bearer <token>" \
     * -H "Content-Type: application/json" \
     * -d '{"repoUrl":"https://github.com/user/repo.git","buildCommand":"npm run
     * build","outputDir":"dist"}'
     */
    @PostMapping("/deploy")
    public ResponseEntity<DeployResponse> deploy(
            @Valid @RequestBody DeployRequest request,
            Authentication auth) {

        // Extract userId from JWT (set by JwtAuthFilter)
        Long userId = (Long) auth.getPrincipal();

        DeployResponse response = deployService.createDeployment(request, userId);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Get deployment status.
     *
     * curl http://localhost:8080/deploy/abc12345 \
     * -H "Authorization: Bearer <token>"
     */
    @GetMapping("/deploy/{id}")
    public ResponseEntity<?> getDeployment(@PathVariable String id) {
        try {
            DeployResponse response = deployService.getDeployment(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/deploy")
    public ResponseEntity<List<DeployResponse>> getAllDeployments(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(deployService.getAllDeployments(userId));
    }

    @DeleteMapping("/deploy/{id}")
    public ResponseEntity<?> deleteDeployment(@PathVariable String id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            deployService.deleteDeployment(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get build logs for a deployment.
     *
     * curl http://localhost:8080/logs/abc12345 \
     * -H "Authorization: Bearer <token>"
     *
     * Returns an array of build steps with their logs,
     * ordered by start time (earliest first).
     */
    @GetMapping("/logs/{deploymentId}")
    public ResponseEntity<?> getLogs(@PathVariable String deploymentId) {
        List<BuildJob> jobs = buildJobRepository
                .findByDeploymentIdOrderByStartedAtAsc(deploymentId);

        if (jobs.isEmpty()) {
            if (deploymentRepository.existsById(deploymentId)) {
                return ResponseEntity.ok(Map.of(
                        "deploymentId", deploymentId,
                        "steps", List.of()));
            }
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Deployment not found: " + deploymentId));
        }

        // Build the response
        List<Map<String, Object>> steps = jobs.stream().map(job -> Map.<String, Object>of(
                "id", job.getId(),
                "step", job.getStep(),
                "status", job.getStatus(),
                "log", job.getLogOutput() != null ? job.getLogOutput() : "",
                "logOutput", job.getLogOutput() != null ? job.getLogOutput() : "",
                "startedAt", job.getStartedAt().toString(),
                "finishedAt", job.getFinishedAt() != null ? job.getFinishedAt().toString() : "")).toList();

        return ResponseEntity.ok(Map.of(
                "deploymentId", deploymentId,
                "steps", steps));
    }

    /**
     * Health check endpoint (public, no auth needed).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
