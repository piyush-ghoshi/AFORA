# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first (better layer caching)
COPY gradlew ./
COPY gradle/ ./gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Ensure gradlew is executable (needed when copied from Windows)
RUN chmod +x gradlew

# Copy source code
COPY src/ ./src/

# Build the fat JAR (skip tests — tests run in CI, not in Docker build)
RUN ./gradlew bootJar -x test --no-daemon

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S afora && adduser -S afora -G afora

WORKDIR /app

# Copy only the fat JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Firebase service account will be provided via FIREBASE_SERVICE_ACCOUNT_JSON env var
# (base64-encoded) — see entrypoint script below
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

USER afora

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
