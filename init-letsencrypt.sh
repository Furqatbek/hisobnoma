#!/bin/bash

# Let's Encrypt SSL certificate initialization script for Hisobnoma
# Usage: sudo ./init-letsencrypt.sh

set -e

DOMAIN="temurmchj.uz"
DOMAIN_WWW="www.temurmchj.uz"
EMAIL="admin@temurmchj.uz"  # Change to your email
COMPOSE_FILE="docker-compose.prod.yml"

echo "=== Hisobnoma SSL Certificate Setup ==="
echo "Domain: $DOMAIN"
echo ""

# Step 1: Create a temporary nginx config that works without SSL
echo "Step 1: Creating temporary HTTP-only nginx config..."
cat > /tmp/default-http-only.conf <<'HTTPCONF'
upstream app_backend {
    server app:8080;
    keepalive 32;
}
upstream frontend_backend {
    server frontend:80;
    keepalive 16;
}
server {
    listen 80;
    server_name temurmchj.uz www.temurmchj.uz;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        proxy_pass http://frontend_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://app_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
HTTPCONF

# Back up current config and use HTTP-only temporarily
cp docker/nginx/conf.d/default.conf docker/nginx/conf.d/default.conf.ssl-backup
cp /tmp/default-http-only.conf docker/nginx/conf.d/default.conf

# Step 2: Start nginx with HTTP-only config
echo "Step 2: Starting nginx with HTTP-only config..."
docker compose -f "$COMPOSE_FILE" up -d nginx

echo "Waiting for nginx to start..."
sleep 5

# Step 3: Obtain the certificate
echo "Step 3: Requesting Let's Encrypt certificate..."
docker compose -f "$COMPOSE_FILE" run --rm certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN" \
    -d "$DOMAIN_WWW"

# Step 4: Restore the full SSL nginx config
echo "Step 4: Restoring full SSL nginx config..."
cp docker/nginx/conf.d/default.conf.ssl-backup docker/nginx/conf.d/default.conf
rm docker/nginx/conf.d/default.conf.ssl-backup

# Step 5: Reload nginx with SSL
echo "Step 5: Reloading nginx with SSL config..."
docker compose -f "$COMPOSE_FILE" exec nginx nginx -s reload

echo ""
echo "=== SSL Setup Complete! ==="
echo "Your site should now be accessible at https://$DOMAIN"
echo ""
echo "Certificate auto-renewal is handled by the certbot container."
echo "To manually renew: docker compose -f $COMPOSE_FILE run --rm certbot renew"
