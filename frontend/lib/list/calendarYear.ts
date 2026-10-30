/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Calendar year string for list filter defaults and query params. */
export function currentCalendarYear(): string {
  return String(new Date().getFullYear());
}
