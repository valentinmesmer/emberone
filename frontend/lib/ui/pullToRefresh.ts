/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Distances are finger travel in CSS pixels, independent of the visual resistance. */
const INTENT_DISTANCE = 20;
const READY_DISTANCE = 144;
const RELEASE_DISTANCE = 116;
const VERTICAL_RATIO = 1.5;

export const PULL_TOP_SETTLE_MS = 300;

export type PullGesture = {
  x: number;
  y: number;
  phase: 'pending' | 'pulling' | 'ready';
  offset: number;
};

export function startPullGesture(x: number, y: number): PullGesture {
  return { x, y, phase: 'pending', offset: 0 };
}

/** Null hands the entire gesture back to scrolling; it cannot re-arm mid-swipe. */
export function movePullGesture(gesture: PullGesture, x: number, y: number): PullGesture | null {
  const dy = y - gesture.y;
  const dx = Math.abs(x - gesture.x);
  if (dy < -INTENT_DISTANCE) return null;
  if (Math.max(dx, Math.abs(dy)) < INTENT_DISTANCE) {
    return { ...gesture, phase: 'pending', offset: 0 };
  }
  if (dy <= 0 || dy < dx * VERTICAL_RATIO) return null;

  // Hysteresis keeps the ready label calm when the finger trembles near the threshold.
  const ready = dy >= (gesture.phase === 'ready' ? RELEASE_DISTANCE : READY_DISTANCE);
  return {
    ...gesture,
    phase: ready ? 'ready' : 'pulling',
    offset: Math.min((dy - INTENT_DISTANCE) * 0.45, 72),
  };
}
