/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

/**
 * Origin of a {@link UserCertificationAssignment}.
 *
 * <ul>
 *   <li>{@link #MANUAL} - direct API assignment by an authorized org user
 *   <li>{@link #WORKFLOW} - created from an approved certification-record workflow (with {@code
 *       sourceAppLinkType}/{@code sourceAppLinkId})
 *   <li>{@link #EVENT} - auto-created from an event-based certification grant ({@code
 *       sourceAppLinkType=CERTIFICATION_EVENT_GRANT}, {@code sourceAppLinkId}=grant id)
 *   <li>{@link #ONBOARDING} - created when an onboarding invitation is approved ({@code
 *       sourceAppLinkType=ONBOARDING_INVITATION}, {@code sourceAppLinkId}=invitation id)
 * </ul>
 */
public enum CertificationAssignmentSource {
    MANUAL,
    WORKFLOW,
    EVENT,
    ONBOARDING
}
