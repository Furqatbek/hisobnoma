#!/bin/sh
set -e

# Replace the backend URL placeholder in nginx config.
# We use a literal placeholder instead of $VAR to avoid conflict with nginx variables ($uri, $host, etc).
API_BACKEND_URL="${API_BACKEND_URL:-http://backend:8080}"

sed -i "s|API_BACKEND_URL_PLACEHOLDER|${API_BACKEND_URL}|g" /etc/nginx/conf.d/default.conf

# Inject the deployment's default tenant into the runtime config. Empty when
# unset, so the pre-login screen falls back to password login (multi-tenant).
DEFAULT_TENANT_ID="${DEFAULT_TENANT_ID:-}"
sed -i "s|DEFAULT_TENANT_ID_PLACEHOLDER|${DEFAULT_TENANT_ID}|g" /usr/share/nginx/html/config.js

echo "Frontend starting - API backend: ${API_BACKEND_URL}, default tenant: ${DEFAULT_TENANT_ID:-<none>}"

exec nginx -g 'daemon off;'
