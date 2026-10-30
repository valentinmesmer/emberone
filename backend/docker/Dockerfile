# syntax=docker/dockerfile:1

# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later

FROM gradle:9-jdk25 AS builder
WORKDIR /workspace/backend

COPY backend/settings.gradle backend/build.gradle backend/gradlew backend/gradlew.bat ./
COPY backend/gradle ./gradle
COPY backend/src ./src
COPY backend/lombok.config ./lombok.config

# Splits the fat jar into layers (dependencies / application) so that a release only
# changes the thin application layer in the registry.
RUN chmod +x gradlew \
  && ./gradlew bootJar -x test --no-daemon \
  && cp build/libs/emberone-backend-*.jar build/app.jar \
  && java -Djarmode=tools -jar build/app.jar extract --layers --destination build/extracted

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache tzdata curl \
  && adduser -S -D -H -u 65532 appuser
ENV TZ=UTC
WORKDIR /app

COPY --from=builder /workspace/backend/build/extracted/dependencies/ ./
COPY --from=builder /workspace/backend/build/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/backend/build/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/backend/build/extracted/application/ ./

ARG APP_VERSION=
ARG SOURCE_REVISION=
ARG SOURCE_REPO=https://github.com/valentinmesmer/emberone
LABEL org.opencontainers.image.source=$SOURCE_REPO
LABEL org.opencontainers.image.version=$APP_VERSION
LABEL org.opencontainers.image.revision=$SOURCE_REVISION

USER appuser
EXPOSE 8080 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+ExitOnOutOfMemoryError", "-XX:+UseG1GC", "-XX:+UseCompactObjectHeaders", "-jar", "/app/app.jar"]
