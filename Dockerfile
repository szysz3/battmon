# Build stage
FROM gradle:8-jdk17 AS builder

WORKDIR /build

# Copy gradle configuration
COPY build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy Docker-specific settings (excludes mobile modules to avoid Android SDK requirement)
COPY settings.docker.gradle.kts ./settings.gradle.kts

# Copy shared module with JVM-only build config (no Android/iOS targets)
COPY shared ./shared
COPY shared/build.gradle.docker.kts ./shared/build.gradle.kts

# Copy backend module
COPY backend ./backend

# Build the backend application
RUN gradle :backend:buildFatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks and calling host services
RUN apk add --no-cache curl

# Copy the built jar from builder stage
COPY --from=builder /build/backend/build/libs/*-all.jar app.jar

# Copy application resources (will be overridden by volume mount or env vars)
COPY backend/src/main/resources/application.yaml ./config/application.yaml

# Expose port (though with host network mode, this is mainly documentation)
EXPOSE 8080

# Set default environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
