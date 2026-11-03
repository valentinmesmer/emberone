/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/** Mirrors backend {@link com.emberone.platform.realtime.event.RealtimeEvent}. */
export type RealtimeEvent = {
  type: string;
  entityIds?: Record<string, number>;
  occurredAt?: string;
  payload?: unknown;
};

export type RealtimeTopicHandler = (event: RealtimeEvent) => void;
