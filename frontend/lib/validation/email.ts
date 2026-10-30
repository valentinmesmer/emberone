/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { EMAIL_PATTERN } from './constants';

/**
 * Non-empty, syntactically valid email (required fields).
 */
export function isValidEmail(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) {
    return false;
  }
  return EMAIL_PATTERN.test(trimmed);
}
