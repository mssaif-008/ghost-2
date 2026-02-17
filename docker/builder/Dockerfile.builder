# ============================================
# Ghost Host — Builder Sandbox Image
# ============================================
# This image is used to run untrusted builds
# inside an isolated container. It includes:
#   - Node.js 20 (for npm/yarn/pnpm builds)
#   - Git (for cloning repos)
#   - Common build tools
#
# Build this image once:
#   docker build -t ghosthost-builder -f Dockerfile.builder .
# ============================================

FROM node:20-alpine

# Install git and common build dependencies
RUN apk add --no-cache \
    git \
    python3 \
    make \
    g++ \
    bash \
    curl

# Create a non-root user for builds
RUN adduser -D -h /workspace builder

# Set working directory
WORKDIR /workspace

# Output directory — worker expects build output here
RUN mkdir -p /output && chown builder:builder /output

# Default: do nothing (the worker overrides CMD)
CMD ["echo", "Builder image ready"]
