# ============================================
# Ghost Host — API Dockerfile
# ============================================
# Multi-stage build:
#   Stage 1: Maven build (large JDK image)
#   Stage 2: Slim JRE runtime (small image)
#
# WHY MULTI-STAGE?
#   The build stage needs Maven + full JDK (~500MB)
#   The runtime only needs JRE (~200MB)
#   Final image is much smaller → faster deploys
# ============================================

# ── Stage 1: Build ──────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml first (Docker layer caching)
# If pom.xml hasn't changed, Maven won't re-download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install Docker CLI so the worker can create build containers
# This is needed because the worker calls `docker run` to build user sites
RUN apk add --no-cache docker-cli

# Copy the built JAR from stage 1
COPY --from=build /app/target/*.jar app.jar

# Create temp directory for build artifacts
RUN mkdir -p /tmp/ghosthost-builds

# Run as non-root user
RUN adduser -D ghosthost
USER ghosthost

EXPOSE 8080

# JVM settings for container environment
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
