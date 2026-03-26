package com.ghosthost.api.worker;

import com.ghosthost.api.entity.BuildJob;
import com.ghosthost.api.entity.Deployment;
import com.ghosthost.api.repository.BuildJobRepository;
import com.ghosthost.api.repository.DeploymentRepository;
import com.ghosthost.api.service.QueueService;
import com.ghosthost.api.service.SupabaseStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * BuildWorker — polls the in-memory queue and orchestrates builds.
 *
 * HOW IT WORKS:
 * 1. Every 2 seconds, we poll the queue for a new deployment ID
 * 2. If we get one, we run the full build pipeline:
 * a. Look up the deployment in the database
 * b. Update status to BUILDING
 * c. Run Docker build (clone → install → build → extract)
 * d. Update status to UPLOADING
 * e. Upload build output to Supabase Storage
 * f. Update status to LIVE
 * g. Clean up temp files and container
 * 3. If any step fails, we set status to FAILED and log the error
 *
 * WHY @Scheduled INSTEAD OF A DEDICATED THREAD?
 * For MVP, @Scheduled is the simplest approach.
 * It runs in a Spring-managed thread pool.
 * The fixedDelay=2000 means "wait 2 seconds AFTER the last execution finished"
 * (not every 2 seconds), so builds won't overlap.
 *
 * WHAT HAPPENS IF THE SERVER RESTARTS?
 * In-memory queue is lost. All QUEUED/BUILDING deployments become stuck.
 * This is a known MVP limitation. With Redis + a recovery job, you'd
 * re-enqueue any stuck deployments on startup.
 */
@Component
public class BuildWorker {

    private static final Logger log = LoggerFactory.getLogger(BuildWorker.class);

    private final QueueService queueService;
    private final DeploymentRepository deploymentRepository;
    private final BuildJobRepository buildJobRepository;
    private final DockerBuildExecutor dockerBuildExecutor;
    private final SupabaseStorageService supabaseStorageService;

    public BuildWorker(QueueService queueService,
            DeploymentRepository deploymentRepository,
            BuildJobRepository buildJobRepository,
            DockerBuildExecutor dockerBuildExecutor,
            SupabaseStorageService supabaseStorageService) {
        this.queueService = queueService;
        this.deploymentRepository = deploymentRepository;
        this.buildJobRepository = buildJobRepository;
        this.dockerBuildExecutor = dockerBuildExecutor;
        this.supabaseStorageService = supabaseStorageService;
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
     * Process a single deployment through the full pipeline.
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

        BuildJob buildLog = logStep(deploymentId, "BUILD", "RUNNING", "Starting build...");

        // Run the Docker build
        DockerBuildExecutor.BuildResult result = dockerBuildExecutor
                .executeBuildWithMount(
                        deploymentId,
                        deployment.getRepoUrl(),
                        deployment.getBuildCommand(),
                        deployment.getOutputDir());

        // Update build log
        buildLog.setLogOutput(result.logs());
        buildLog.setFinishedAt(LocalDateTime.now());

        if (!result.success()) {
            buildLog.setStatus("FAILED");
            buildJobRepository.save(buildLog);
            failDeployment(deploymentId, "Build failed. See logs for details.");
            return;
        }

        buildLog.setStatus("SUCCESS");
        buildJobRepository.save(buildLog);

        // ── Step: UPLOADING ─────────────────────────────
        deployment.setStatus("UPLOADING");
        deploymentRepository.save(deployment);

        BuildJob uploadLog = logStep(deploymentId, "UPLOAD", "RUNNING", "Uploading to Supabase...");

        try {
            int fileCount = supabaseStorageService.uploadDirectory(
                    result.outputPath(), deploymentId);

            uploadLog.setLogOutput("Uploaded " + fileCount + " files to Supabase");
            uploadLog.setStatus("SUCCESS");
            uploadLog.setFinishedAt(LocalDateTime.now());
            buildJobRepository.save(uploadLog);

        } catch (Exception e) {
            log.error("[{}] Upload failed: {}", deploymentId, e.getMessage(), e);
            uploadLog.setLogOutput("Upload error: " + e.getMessage());
            uploadLog.setStatus("FAILED");
            uploadLog.setFinishedAt(LocalDateTime.now());
            buildJobRepository.save(uploadLog);
            failDeployment(deploymentId, "Upload failed: " + e.getMessage());
            return;
        }

        // ── Step: LIVE ──────────────────────────────────
        deployment.setStatus("LIVE");
        deploymentRepository.save(deployment);
        log.info("[{}] 🎉 Deployment is LIVE at: {}", deploymentId, deployment.getSiteUrl());

        // ── Cleanup ─────────────────────────────────────
        dockerBuildExecutor.cleanup(result.outputPath());
        logStep(deploymentId, "CLEANUP", "SUCCESS", "Cleaned up build artifacts");
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
