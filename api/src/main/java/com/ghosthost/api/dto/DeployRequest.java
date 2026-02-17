package com.ghosthost.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /deploy
 *
 * Either repoUrl (for GitHub) or zipPath (for uploaded ZIP) must be provided.
 */
public class DeployRequest {

    private String repoUrl;       // GitHub repo URL (optional if ZIP)

    @NotBlank(message = "buildCommand is required")
    private String buildCommand;  // e.g. "npm install && npm run build"

    @NotBlank(message = "outputDir is required")
    private String outputDir;     // e.g. "dist" or "build"

    // ── Getters & Setters ─────────────────────
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getBuildCommand() { return buildCommand; }
    public void setBuildCommand(String buildCommand) { this.buildCommand = buildCommand; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
}
