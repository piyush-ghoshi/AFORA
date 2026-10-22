#!/bin/sh
# docker-entrypoint.sh
# Handles Firebase credentials injection from environment variable at runtime.
# This avoids committing the firebase-service-account.json to the Docker image.

set -e

# If FIREBASE_SERVICE_ACCOUNT_JSON env var is set (base64-encoded JSON),
# decode it and write it to the expected location.
if [ -n "$FIREBASE_SERVICE_ACCOUNT_JSON" ]; then
    echo "[entrypoint] Writing Firebase service account from environment..."
    echo "$FIREBASE_SERVICE_ACCOUNT_JSON" | base64 -d > /tmp/firebase-service-account.json
    export FIREBASE_SA_PATH="file:/tmp/firebase-service-account.json"
else
    # Fallback: use classpath-bundled file (only for local Docker runs)
    echo "[entrypoint] FIREBASE_SERVICE_ACCOUNT_JSON not set — using bundled classpath file."
    export FIREBASE_SA_PATH="classpath:firebase-service-account.json"
fi

exec java \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
  -Dfirebase.service-account-key=${FIREBASE_SA_PATH} \
  -Dserver.port=${PORT:-8080} \
  -jar /app/app.jar
