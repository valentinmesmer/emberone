/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Mirrors backend {@link com.emberone.platform.realtime.event.RealtimeEvent}. */
export type RealtimeEvent = {
  type: string;
  entityIds?: Record<string, number>;
  occurredAt?: string;
  payload?: unknown;
};

export type RealtimeTopicHandler = (event: RealtimeEvent) => void;
