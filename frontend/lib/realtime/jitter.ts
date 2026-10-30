/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Spreads {@code baseMs} by ±{@code fraction} (e.g. 0.3 → ±30%). */
export function withJitter(baseMs: number, fraction = 0.3): number {
  const spread = baseMs * fraction;
  return baseMs - spread + Math.random() * 2 * spread;
}
