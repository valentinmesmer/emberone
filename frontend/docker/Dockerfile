# syntax=docker/dockerfile:1

# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later

FROM oven/bun:1.4.0-alpine AS base

FROM base AS deps
WORKDIR /app
COPY frontend/package.json frontend/bun.lock ./
RUN bun install --frozen-lockfile

FROM base AS builder
WORKDIR /app
ARG APP_VERSION=
ARG GIT_TAG=
ARG SOURCE_REVISION=
ARG SOURCE_REPO=https://github.com/valentinmesmer/emberone
ENV NEXT_TELEMETRY_DISABLED=1
ENV NEXT_PUBLIC_APP_VERSION=$APP_VERSION
ENV NEXT_PUBLIC_GIT_TAG=$GIT_TAG
ENV NEXT_PUBLIC_SOURCE_REVISION=$SOURCE_REVISION
ENV NEXT_PUBLIC_SOURCE_REPO=$SOURCE_REPO
COPY --from=deps /app/node_modules ./node_modules
COPY frontend/ .
RUN bun run build

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV TZ=UTC

RUN apk add --no-cache su-exec tzdata curl \
  && addgroup --system --gid 1001 app \
  && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

COPY frontend/docker/write-runtime-config.cjs /write-runtime-config.cjs
COPY frontend/docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh \
  && chown -R nextjs:app /app/public

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:3000/healthz || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["bun", "server.js"]
