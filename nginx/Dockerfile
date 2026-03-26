# ============================================
# Ghost Host — Nginx Reverse Proxy
# ============================================
FROM nginx:1.25-alpine

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom config as a template for envsubst
COPY default.conf.template /etc/nginx/templates/default.conf.template

# Create directory for SSL certs (user mounts or copies them)
RUN mkdir -p /etc/nginx/certs

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
