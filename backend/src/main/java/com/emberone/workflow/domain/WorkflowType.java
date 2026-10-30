/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stable workflow type discriminator (DB, API, type mappings). Each value is also the approver-role
 * key for {@code wf_type_mappings} (see workflows.md).
 */
@Getter
@RequiredArgsConstructor
public enum WorkflowType {

    /** Request to change first and/or last name, including a proof document. */
    NAME_CHANGE(1L),

    /** Request to change the private address; proof document is optional. */
    ADDRESS_CHANGE(2L),

    /** Submission of a certification proof (definition + completion date + proof document). */
    CERTIFICATION_RECORD(3L),

    /**
     * Submission of a training course proof (type + date range + optional hours + proof document).
     */
    TRAINING_COURSE_RECORD(4L),

    /**
     * Submission of a driving license proof (classes + issue date + optional metadata + proof
     * document).
     */
    DRIVING_LICENSE_RECORD(5L),

    /** Request to join a second organization (dual approval: main org + target org). */
    ORGANIZATION_JOIN_REQUEST(6L),

    /**
     * Request to change the main organization (dual approval: current main org + new main org; only
     * the new main org when none is set).
     */
    MAIN_ORGANIZATION_CHANGE(7L),

    /** Request to leave an organization (approver: the organization being left). */
    ORGANIZATION_LEAVE_REQUEST(8L),

    /** Request to leave an organization group (approver: the organization that owns the group). */
    ORGANIZATION_GROUP_LEAVE_REQUEST(9L),

    /** Request to join an organization group (approver: the organization that owns the group). */
    ORGANIZATION_GROUP_JOIN_REQUEST(10L),

    /** Submit completed onboarding for review by the target organization. */
    ONBOARDING_SUBMISSION(11L),

    /**
     * Org user write: request ending another member's organization membership (approver: context
     * organization).
     */
    ORGANIZATION_MEMBERSHIP_END_REQUEST(13L),

    /**
     * Org user write: request changing another member's main organization (dual approval on target
     * user).
     */
    ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE(14L),

    /** Self-service: submit a personal document for approval before it is stored in the archive. */
    USER_DOCUMENT_SUBMISSION(15L),

    /**
     * Self-service: request the first rank while none is assigned (approver: main org leadership).
     */
    RANK_REQUEST(16L),

    /**
     * Self-service: register for a published course offering (approver: the offering's
     * organization, plus the user's main organization when the offering requires its approval).
     */
    COURSE_OFFERING_REGISTRATION(17L),

    /**
     * Self-service: cancel a course offering participation before the first session (approvers: the
     * offering's organization and the user's main organization, always both).
     */
    COURSE_REGISTRATION_CANCELLATION(18L);

    private final Long id;

    /** Self-service (Mein Bereich) vs org {@code USER_WRITE} management workflows. */
    public WorkflowOpenScope openScope() {
        return switch (this) {
            case ORGANIZATION_MEMBERSHIP_END_REQUEST, ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE ->
                    WorkflowOpenScope.ORGANIZATION;
            default -> WorkflowOpenScope.SELF;
        };
    }

    @JsonCreator
    public static WorkflowType fromId(Number id) {
        if (id == null) {
            return null;
        }
        long lid = id.longValue();
        for (WorkflowType type : values()) {
            if (type.id == lid) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown WorkflowType id: " + id);
    }

    private static WorkflowType tryFromId(Number id) {
        if (id == null) {
            return null;
        }
        long lid = id.longValue();
        for (WorkflowType type : values()) {
            if (type.id == lid) {
                return type;
            }
        }
        return null;
    }

    public static boolean isKnownId(Number id) {
        return tryFromId(id) != null;
    }

    @JsonValue
    public Long getId() {
        return id;
    }
}
