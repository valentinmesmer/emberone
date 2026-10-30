/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Shared validation constants aligned with backend ValidationPatterns.
 */

/** ASCII-only email: local@domain.tld (umlauts etc. are rejected by the identity provider) */
export const EMAIL_PATTERN =
  /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z0-9-]+$/;

/** Default region for national phone numbers (ISO 3166-1 alpha-2). */
export const DEFAULT_PHONE_REGION = 'DE' as const;

/** Uppercase letters, digits, underscore. */
export const ORGANIZATION_CODE_PATTERN = /^[A-Z0-9_]+$/;

export const ORGANIZATION_CODE_MAX_LENGTH = 5;

/**
 * Maximum age in years for a date of birth. Rejects incomplete years (e.g. year 4 instead of 2004)
 * and implausible historical dates. Aligned with backend ValidationPatterns.
 */
export const DATE_OF_BIRTH_MAX_AGE_YEARS = 150;

/** EmberOne application roles accepted by the admin API. */
export const APP_ROLE_PATTERN = /^(USER|TENANT_ADMIN|GLOBAL_ADMIN)$/;
