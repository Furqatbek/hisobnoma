# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy entrypoint that injects API_BACKEND_URL at runtime
# sed strips Windows \r line endings (git autocrlf on Windows adds them)
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Default backend URL (overridden by docker-compose environment)
ENV API_BACKEND_URL=http://backend:8080

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
