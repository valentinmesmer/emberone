/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

/** Computed (never stored) status of a required certification for a user. */
public enum CertificationStatus {
    /** No record present but the requirement is active. */
    MISSING,
    /** A submission workflow is open and currently being reviewed. */
    IN_REVIEW,
    /** A record exists but its expiry date is in the past. */
    EXPIRED,
    /** A record exists and expires within the definition's reminder window. */
    EXPIRING_SOON,
    /** A record exists and is valid (never expires or beyond the reminder window). */
    VALID,
    /** No longer required; historical records remain visible. */
    ARCHIVED
}
