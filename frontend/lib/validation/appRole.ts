/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { APP_ROLE_PATTERN } from './constants';

export function isValidAppRole(value: string): boolean {
  return APP_ROLE_PATTERN.test(value.trim().toUpperCase());
}
