/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

/** Shared validation patterns aligned with the frontend ({@code frontend/lib/validation/}). */
public final class ValidationPatterns {

    /**
     * ASCII-only email check: local@domain.tld. Non-ASCII characters (e.g. umlauts) are rejected
     * because the identity provider does not accept them. Mirrors {@code EMAIL_PATTERN} in the
     * frontend.
     */
    public static final String EMAIL =
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z0-9-]+$";

    /** Default ISO 3166-1 alpha-2 region for national phone numbers. */
    public static final String DEFAULT_PHONE_REGION = "DE";

    /** Uppercase letters, digits, underscore. */
    public static final String ORGANIZATION_CODE = "^[A-Z0-9_]+$";

    public static final int ORGANIZATION_CODE_MAX_LENGTH = 5;

    /**
     * Maximum age in years for a date of birth. Rejects incomplete years (e.g. year 4 instead of
     * 2004) and implausible historical dates. Aligned with the frontend.
     */
    public static final int DATE_OF_BIRTH_MAX_AGE_YEARS = 150;

    /** EmberOne application roles accepted by the admin API. */
    public static final String APP_ROLE = "USER|TENANT_ADMIN|GLOBAL_ADMIN";

    private ValidationPatterns() {}
}
