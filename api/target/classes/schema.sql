-- ============================================
-- Ghost Host — Database Schema
-- ============================================
-- This file runs on every application startup.
-- CREATE IF NOT EXISTS ensures it's idempotent.
-- ============================================

-- USERS table
-- Stores registered users. Password is BCrypt hashed.
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- DEPLOYMENTS table
-- One row per deployment request. The `id` is also used as the subdomain.
-- Status transitions: QUEUED → BUILDING → UPLOADING → LIVE
--                     Any state → FAILED
CREATE TABLE IF NOT EXISTS deployments (
    id             VARCHAR(36) PRIMARY KEY,
    user_id        BIGINT REFERENCES users(id),
    repo_url       VARCHAR(2048),
    zip_path       VARCHAR(2048),
    build_command  VARCHAR(1024) NOT NULL,
    output_dir     VARCHAR(512)  NOT NULL,
    status         VARCHAR(50)   NOT NULL DEFAULT 'QUEUED',
    site_url       VARCHAR(2048),
    error_message  TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- BUILD_JOBS table
-- Stores logs for each step of the build pipeline.
-- One deployment has multiple build_jobs (one per step).
CREATE TABLE IF NOT EXISTS build_jobs (
    id             BIGSERIAL PRIMARY KEY,
    deployment_id  VARCHAR(36) REFERENCES deployments(id),
    step           VARCHAR(100) NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    log_output     TEXT,
    started_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at    TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_deployments_user_id ON deployments(user_id);
CREATE INDEX IF NOT EXISTS idx_deployments_status ON deployments(status);
CREATE INDEX IF NOT EXISTS idx_build_jobs_deployment_id ON build_jobs(deployment_id);
