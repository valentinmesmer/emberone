/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/** 0-1 progress - drives green/orange split visuals (success vs warning tokens). */
export function successWarningSplitPercent(ratio: number): number {
  if (!Number.isFinite(ratio)) return 0;
  return Math.min(100, Math.max(0, Math.round(ratio * 100)));
}

export function successWarningSplitSurfaceBackground(ratio: number): string {
  const pct = successWarningSplitPercent(ratio);
  return `linear-gradient(to right, var(--status-success-surface) 0%, var(--status-success-surface) ${pct}%, var(--status-warning-surface) ${pct}%, var(--status-warning-surface) 100%)`;
}
