/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

/**
 * Configurable evidence types for a tenant certification definition. Multiple types can be combined
 * (set, not single choice).
 *
 * <ul>
 *   <li>Empty set - self-service / manual without attachment (date confirmation)
 *   <li>{@link #DOCUMENT} - self-service / manual with required attachment
 *   <li>{@link #SERVICE_EVENT} / {@link #OPERATION_EVENT} - event-based grant
 * </ul>
 *
 * Event-only (only service/operation) blocks self-service and manual entry.
 */
public enum CertEvidenceType {
    DOCUMENT,
    SERVICE_EVENT,
    OPERATION_EVENT
}
