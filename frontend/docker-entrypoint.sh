#!/bin/sh
set -e

# Replace the backend URL placeholder in nginx config.
# We use a literal placeholder instead of $VAR to avoid conflict with nginx variables ($uri, $host, etc).
API_BACKEND_URL="${API_BACKEND_URL:-http://backend:8080}"

sed -i "s|API_BACKEND_URL_PLACEHOLDER|${API_BACKEND_URL}|g" /etc/nginx/conf.d/default.conf

echo "Frontend starting — API backend: ${API_BACKEND_URL}"

exec nginx -g 'daemon off;'
