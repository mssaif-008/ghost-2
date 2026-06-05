package com.ghosthost.api.worker;

import com.ghosthost.api.entity.BuildJob;
import com.ghosthost.api.entity.Deployment;
import com.ghosthost.api.repository.BuildJobRepository;
import com.ghosthost.api.repository.DeploymentRepository;
import com.ghosthost.api.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * BuildWorker — polls the in-memory queue and orchestrates builds.
 *
 * HOW IT WORKS NOW (GitHub Actions Integration):
 * 1. Every 2 seconds, we poll the queue for a new deployment ID
 * 2. If we get one, we look up the deployment in the database
 * 3. We update status to BUILDING
 * 4. We trigger a GitHub Actions workflow via the Repository Dispatch API
 * 5. We stop processing locally. The GHA workflow will update the status
 *    to LIVE or FAILED via the internal callback API.
 */
@Component
public class BuildWorker {

    private static final Logger log = LoggerFactory.getLogger(BuildWorker.class);

    private final QueueService queueService;
    private final DeploymentRepository deploymentRepository;
    private final BuildJobRepository buildJobRepository;
    private final RestTemplate restTemplate;

    @Value("${github.pat}")
    private String githubPat;

    @Value("${github.repo}")
    private String githubRepo;

    @Value("${app.api-url}")
    private String apiUrl;

    public BuildWorker(QueueService queueService,
            DeploymentRepository deploymentRepository,
            BuildJobRepository buildJobRepository) {
        this.queueService = queueService;
        this.deploymentRepository = deploymentRepository;
        this.buildJobRepository = buildJobRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Poll the queue every 2 seconds and process the next job.
     */
    @Scheduled(fixedDelay = 2000)
    public void pollAndBuild() {
        // Try to get the next deployment from the queue
        String deploymentId = queueService.poll();
        if (deploymentId == null) {
            return; // No jobs waiting — do nothing
        }

        log.info("========================================");
        log.info("Processing deployment: {}", deploymentId);
        log.info("========================================");

        try {
            processDeployment(deploymentId);
        } catch (Exception e) {
            log.error("Unexpected error processing deployment {}: {}",
                    deploymentId, e.getMessage(), e);
            failDeployment(deploymentId, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Process a single deployment by triggering GitHub Actions.
     */
    private void processDeployment(String deploymentId) {
        // 1. Look up the deployment
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElse(null);

        if (deployment == null) {
            log.error("Deployment not found: {}", deploymentId);
            return;
        }

        // ── Step: BUILDING ──────────────────────────────
        deployment.setStatus("BUILDING");
        deploymentRepository.save(deployment);

        BuildJob buildLog = logStep(deploymentId, "DISPATCH", "RUNNING", "Triggering GitHub Actions build...");

        try {
            // Trigger GitHub Action
            triggerGitHubAction(deployment);

            buildLog.setLogOutput("Successfully triggered GitHub Actions workflow.");
            buildLog.setStatus("SUCCESS");
            buildLog.setFinishedAt(LocalDateTime.now());
            buildJobRepository.save(buildLog);

            log.info("[{}] Triggered GitHub Actions build.", deploymentId);
            
            // Note: We don't change state to LIVE here. The GitHub Action will do it.
        } catch (Exception e) {
            log.error("[{}] Failed to trigger GitHub Actions: {}", deploymentId, e.getMessage(), e);
            buildLog.setLogOutput("Failed to trigger GitHub Actions: " + e.getMessage());
            buildLog.setStatus("FAILED");
            buildLog.setFinishedAt(LocalDateTime.now());
            buildJobRepository.save(buildLog);
            
            failDeployment(deploymentId, "Failed to start build on GitHub Actions.");
        }
    }

    private void triggerGitHubAction(Deployment deployment) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubPat);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        String callbackUrl = apiUrl + "/api/internal/deployments/" + deployment.getId() + "/status";

        Map<String, Object> payload = Map.of(
                "deployment_id", deployment.getId(),
                "repo_url", deployment.getRepoUrl() != null ? deployment.getRepoUrl() : "",
                "build_command", deployment.getBuildCommand() != null ? deployment.getBuildCommand() : "",
                "output_dir", deployment.getOutputDir() != null ? deployment.getOutputDir() : "",
                "callback_url", callbackUrl
        );

        Map<String, Object> body = Map.of(
                "event_type", "build-site",
                "client_payload", payload
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String dispatchUrl = "https://api.github.com/repos/" + githubRepo + "/dispatches";
        
        log.info("Sending repository_dispatch to {}", dispatchUrl);
        restTemplate.postForEntity(dispatchUrl, entity, Void.class);
    }

    /**
     * Mark a deployment as failed.
     */
    private void failDeployment(String deploymentId, String errorMessage) {
        deploymentRepository.findById(deploymentId).ifPresent(d -> {
            d.setStatus("FAILED");
            d.setErrorMessage(errorMessage);
            deploymentRepository.save(d);
        });
        log.error("[{}] ❌ Deployment FAILED: {}", deploymentId, errorMessage);
    }

    /**
     * Create a build job log entry.
     */
    private BuildJob logStep(String deploymentId, String step, String status, String message) {
        BuildJob job = new BuildJob(deploymentId, step, status);
        job.setLogOutput(message);
        return buildJobRepository.save(job);
    }
}

