/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Merges explicit and default messages for disabled / loading action controls.
 * Used with HoverHint in UI primitives (not native `title`).
 */
export function mergeActionHoverTitle(
  nonInteractive: boolean,
  loading: boolean,
  explicitTitle: string | undefined,
  messages: { loading: string; unavailable: string },
): string | undefined {
  if (!nonInteractive) return explicitTitle;
  if (explicitTitle?.trim()) return explicitTitle;
  return loading ? messages.loading : messages.unavailable;
}
