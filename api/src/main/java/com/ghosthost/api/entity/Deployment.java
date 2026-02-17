package com.ghosthost.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Deployment entity — one row per deploy request.
 *
 * The `id` field (UUID string) doubles as the subdomain:
 *   https://{id}.mydomain.com
 *
 * Status transitions:
 *   QUEUED → BUILDING → UPLOADING → LIVE
 *   Any state → FAILED (on error)
 */
@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    @Column(length = 36)
    private String id;              // UUID, also the subdomain

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "repo_url", length = 2048)
    private String repoUrl;

    @Column(name = "zip_path", length = 2048)
    private String zipPath;

    @Column(name = "build_command", nullable = false, length = 1024)
    private String buildCommand;

    @Column(name = "output_dir", nullable = false, length = 512)
    private String outputDir;

    @Column(nullable = false, length = 50)
    private String status = "QUEUED";

    @Column(name = "site_url", length = 2048)
    private String siteUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ── Constructors ──────────────────────────
    public Deployment() {}

    // ── Getters & Setters ─────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getZipPath() { return zipPath; }
    public void setZipPath(String zipPath) { this.zipPath = zipPath; }

    public String getBuildCommand() { return buildCommand; }
    public void setBuildCommand(String buildCommand) { this.buildCommand = buildCommand; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
