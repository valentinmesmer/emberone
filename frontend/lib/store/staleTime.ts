/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/**
 * Freshness tiers for server state. Use these instead of numeric literals so equal kinds of
 * data share one value. Realtime events and mutations invalidate queries independently; the
 * tier only bounds how stale data may get when no such signal arrives.
 */
export const STALE_TIME = {
  /** Work in progress that other users change often (workflow decisions, todo counts). */
  REALTIME: 15 * 1000,
  /** Inboxes and overviews that are usually refreshed by realtime events. */
  LIVE: 30 * 1000,
  /** Records and lists of a single user or organization. */
  LIST: 60 * 1000,
  /** Catalogs, definitions and profile data that change rarely. Also the client default. */
  STANDARD: 5 * 60 * 1000,
} as const;
