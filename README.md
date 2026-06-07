Demo Repo to deploy- https://github.com/mssaif-008/for-ghost.git

# 👻 Ghost Host

> **Deploy without ceremony** — a lightweight, self-hosted static deployment platform for frontend applications.

Ghost Host lets you push a repository URL, specify a build command and output directory, and get back a live URL — no server setup, no SSH, no ceremony.

---

## 🚀 What It Does

- **Builds** your project in isolated GitHub Actions environments
- **Generates** a unique subdomain for every deployment
- **Hosts** your built assets on Supabase Storage
- **Streams** real-time build logs so you always know what's happening

---

## 🏗️ Architecture

```
User → Frontend (Vercel)
           ↓
      REST API (Render)
           ↓
   GitHub Actions (Build)
           ↓
  Supabase Storage (Assets)
  Supabase Postgres (DB)
```

**Deployment lifecycle:**
1. User submits repo URL + build config via the React frontend
2. API queues a build job and triggers a GitHub Actions workflow
3. The workflow clones the repo, runs the build command in a Node 20 container, and extracts the output directory
4. Built assets are uploaded to Supabase Storage under a UUID-based path
5. A unique subdomain is assigned and the deployment is marked live
6. Build logs stream back to the frontend step-by-step (CLONE → INSTALL → BUILD → EXTRACT → UPLOAD)

---

## 🛠️ Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.3 (Java 17) |
| Database ORM | JPA / Hibernate |
| Auth | JWT (JJWT 0.12.5) + BCrypt |
| Validation | Hibernate Validator |
| Build Tool | Maven |
| Hosting | Render |

### Frontend
| Layer | Technology |
|---|---|
| Framework | React 19 + Vite |
| Routing | React Router 6 |
| HTTP Client | Axios |
| Hosting | Vercel |

### Infrastructure
| Concern | Service |
|---|---|
| Database | Supabase Postgres |
| Asset Storage | Supabase Storage |
| Build Execution | GitHub Actions (Node 20) |

---

## 📦 Local Development

### Prerequisites
- Java 17+
- Node.js 20+
- Docker & Docker Compose
- A Supabase project
- A GitHub account (for Actions integration)

### Environment Variables

**Backend (`application.properties` or env):**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://<supabase-host>:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<your-password>
JWT_SECRET=<your-secret>
GITHUB_TOKEN=<your-pat>
GITHUB_REPO_OWNER=<owner>
GITHUB_REPO_NAME=<repo>
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_KEY=<service-role-key>
SUPABASE_BUCKET=deployments
```

**Frontend (`.env`):**
```
VITE_API_BASE_URL=http://localhost:8080
```

### Run Locally

```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

---

## 🔐 Security

- Stateless JWT authentication — no server-side sessions
- BCrypt password hashing
- Build execution is fully isolated in ephemeral GitHub Actions runners
- CORS configured to allow only known frontend origins

**Known gaps (planned fixes):**
- No rate limiting on auth endpoints
- GitHub PAT used instead of GitHub App (lower permission surface planned)
- No deployment rollback (on roadmap)
- No audit logging

---

## 📊 Database Schema (simplified)

```
users
  id, email, password_hash, created_at

deployments
  id (UUID), user_id, repo_url, build_command,
  output_dir, subdomain, status, created_at, updated_at

build_jobs
  id, deployment_id, step (CLONE|INSTALL|BUILD|EXTRACT|UPLOAD),
  status, log_output, started_at, finished_at
```

---

## 🔮 Roadmap

**High priority**
- [ ] Persistent job queue (Redis / RabbitMQ) to survive restarts
- [ ] Build timeouts and per-user resource limits
- [ ] Deployment rollback (keep last 5 versions)
- [ ] Rate limiting and per-user deployment quotas

**Medium priority**
- [ ] GitHub App integration (replace PAT)
- [ ] Custom domain support with SSL (Let's Encrypt)
- [ ] Environment variable injection for builds
- [ ] Email notifications on success / failure
- [ ] Log retention policy (auto-purge after 90 days)

**Future**
- [ ] Team collaboration with role-based access
- [ ] Monorepo support
- [ ] Build caching
- [ ] Lighthouse / performance analytics

---

## 🆚 How It Compares

| Feature | Ghost Host | Vercel | Netlify |
|---|---|---|---|
| Cost | Cheap (self-hosted) | $20+/mo | Free tier limited |
| Vendor lock-in | None | Yes | Yes |
| Live build logs | ✅ | ✅ | ✅ |
| Rollbacks | ❌ planned | ✅ | ✅ |
| Custom domains | ❌ planned | ✅ | ✅ |
| Preview deploys | ❌ | ✅ | ✅ |

---

## 🎯 Who It's For

**Great fit:**
- Freelancers hosting client projects
- Students and side-project builders
- Teams wanting self-hosted deployments without vendor lock-in
- Portfolio sites needing instant URLs

**Not the right tool for:**
- Enterprise apps requiring SLAs
- Complex multi-environment pipelines
- Teams needing vendor support

---

## 📝 License

MIT
