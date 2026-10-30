/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { ORGANIZATION_CODE_MAX_LENGTH, ORGANIZATION_CODE_PATTERN } from './constants';

export function normalizeOrganizationCode(code: string): string {
  return code.trim().toUpperCase();
}

export function isValidOrganizationCode(value: string): boolean {
  const normalized = normalizeOrganizationCode(value);
  if (!normalized) {
    return false;
  }
  if (normalized.length > ORGANIZATION_CODE_MAX_LENGTH) {
    return false;
  }
  return ORGANIZATION_CODE_PATTERN.test(normalized);
}

/** Restrict input to allowed organization-code characters. */
export function sanitizeOrganizationCodeInput(value: string): string {
  return value
    .toUpperCase()
    .replace(/[^A-Z0-9_]/g, '')
    .slice(0, ORGANIZATION_CODE_MAX_LENGTH);
}
