package com.ghosthost.api.service;

import com.ghosthost.api.dto.DeployRequest;
import com.ghosthost.api.dto.DeployResponse;
import com.ghosthost.api.entity.Deployment;
import com.ghosthost.api.repository.DeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deploy service — handles creating deployments and retrieving status.
 *
 * FLOW:
 * 1. User calls POST /deploy with repo URL + build command + output dir
 * 2. We create a Deployment row with status=QUEUED
 * 3. We enqueue the deployment ID for the worker to pick up
 * 4. We return the deployment info immediately (202 Accepted)
 * 5. The worker picks it up asynchronously and builds it
 */
@Service
public class DeployService {

    private static final Logger log = LoggerFactory.getLogger(DeployService.class);

    private final DeploymentRepository deploymentRepository;
    private final QueueService queueService;

    @Value("${app.base-domain}")
    private String baseDomain;

    public DeployService(DeploymentRepository deploymentRepository,
            QueueService queueService) {
        this.deploymentRepository = deploymentRepository;
        this.queueService = queueService;
    }

    /**
     * Create a new deployment and enqueue it for building.
     */
    public DeployResponse createDeployment(DeployRequest request, Long userId) {
        // 1. Generate a unique deployment ID (also used as subdomain)
        // Using first 8 chars of UUID for shorter subdomains
        String fullUuid = UUID.randomUUID().toString();
        String deploymentId = fullUuid.substring(0, 8);

        // 2. Create Deployment entity
        Deployment deployment = new Deployment();
        deployment.setId(deploymentId);
        deployment.setUserId(userId);
        deployment.setRepoUrl(request.getRepoUrl());
        deployment.setBuildCommand(request.getBuildCommand());
        deployment.setOutputDir(request.getOutputDir());
        deployment.setStatus("QUEUED");
        deployment.setSiteUrl(buildSiteUrl(deploymentId));

        // 3. Save to database
        deployment = deploymentRepository.save(deployment);
        log.info("Created deployment {} for user {}", deploymentId, userId);

        // 4. Enqueue for the worker
        boolean enqueued = queueService.enqueue(deploymentId);
        if (!enqueued) {
            deployment.setStatus("FAILED");
            deployment.setErrorMessage("Build queue is full. Try again later.");
            deploymentRepository.save(deployment);
            log.error("Queue full! Failed to enqueue deployment {}", deploymentId);
        }

        // 5. Return response
        return toResponse(deployment);
    }

    /**
     * Get deployment status by ID.
     */
    public DeployResponse getDeployment(String deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deployment not found: " + deploymentId));
        return toResponse(deployment);
    }

    public java.util.List<DeployResponse> getAllDeployments(Long userId) {
        return deploymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteDeployment(String deploymentId, Long userId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));
        if (!deployment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to delete this deployment");
        }
        deploymentRepository.delete(deployment);
    }

    /**
     * Convert entity to response DTO.
     */
    private DeployResponse toResponse(Deployment d) {
        DeployResponse response = new DeployResponse();
        response.setDeploymentId(d.getId());
        response.setStatus(d.getStatus());
        response.setRepoUrl(d.getRepoUrl());
        response.setBuildCommand(d.getBuildCommand());
        response.setOutputDir(d.getOutputDir());
        response.setSiteUrl(d.getSiteUrl());
        response.setErrorMessage(d.getErrorMessage());
        response.setCreatedAt(d.getCreatedAt());
        response.setUpdatedAt(d.getUpdatedAt());
        return response;
    }

    private String buildSiteUrl(String deploymentId) {
        String normalizedBaseDomain = baseDomain == null ? "localhost" : baseDomain.trim();
        if (normalizedBaseDomain.startsWith("http://")) {
            return "http://" + deploymentId + "."
                    + normalizedBaseDomain.substring("http://".length());
        }
        if (normalizedBaseDomain.startsWith("https://")) {
            return "https://" + deploymentId + "."
                    + normalizedBaseDomain.substring("https://".length());
        }
        return "http://" + deploymentId + "." + normalizedBaseDomain;
    }
}
