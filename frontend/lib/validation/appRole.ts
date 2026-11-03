/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { APP_ROLE_PATTERN } from './constants';

export function isValidAppRole(value: string): boolean {
  return APP_ROLE_PATTERN.test(value.trim().toUpperCase());
}
