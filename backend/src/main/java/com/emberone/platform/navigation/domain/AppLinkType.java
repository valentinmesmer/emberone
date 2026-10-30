/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.navigation.domain;

/**
 * Semantic in-app link target shared by documents, notifications and to-dos. Persisted as {@code
 * link_type} + {@code link_id}; routes are resolved at read time.
 */
public enum AppLinkType {
    WORKFLOW,
    CERTIFICATION_RECORD,
    TRAINING_COURSE_RECORD,
    DRIVING_LICENSE_RECORD,
    /**
     * Event-based certification grant. {@code linkId} = grant id; resolves to the event detail page
     * via {@code CertificationEventGrantNavigationResolver} (not the grant-admin detail).
     */
    CERTIFICATION_EVENT_GRANT,

    /** Approved onboarding invitation; {@code linkId} = invitation id. */
    ONBOARDING_INVITATION,

    /**
     * Course credited by the completion of a course offering; {@code linkId} = course registration
     * id (the evidence of the credit, retained with it under {@code USER_MASTER}). Has no in-app
     * route and never carries documents; a generated document would link the training course record
     * instead.
     */
    COURSE_REGISTRATION
}
