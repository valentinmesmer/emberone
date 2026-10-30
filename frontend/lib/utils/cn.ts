/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** Merge Tailwind class names; prefer this module over the `@/lib/utils` barrel from UI primitives. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
