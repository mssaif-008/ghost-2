package com.ghosthost.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * BuildJob entity — stores logs for each step of the build pipeline.
 *
 * Each deployment can have multiple BuildJob rows, one per step:
 *   CLONE → INSTALL → BUILD → EXTRACT → UPLOAD
 *
 * The `logOutput` field captures stdout/stderr from each step.
 */
@Entity
@Table(name = "build_jobs")
public class BuildJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", length = 36)
    private String deploymentId;

    @Column(nullable = false, length = 100)
    private String step;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "log_output", columnDefinition = "TEXT")
    private String logOutput;

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // ── Constructors ──────────────────────────
    public BuildJob() {}

    public BuildJob(String deploymentId, String step, String status) {
        this.deploymentId = deploymentId;
        this.step = step;
        this.status = status;
    }

    // ── Getters & Setters ─────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }

    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLogOutput() { return logOutput; }
    public void setLogOutput(String logOutput) { this.logOutput = logOutput; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
