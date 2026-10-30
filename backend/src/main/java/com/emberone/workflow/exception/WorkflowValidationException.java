/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.exception;

import com.emberone.platform.exception.ValidationException;

/**
 * Client-facing validation failures for workflow APIs. Optional {@link #getCode()} is stable for
 * i18n / UI mapping.
 */
public class WorkflowValidationException extends ValidationException {

    /**
     * User already has an open workflow of this type (same tenant); see handler {@code
     * allowsMultipleConcurrentOpen()}.
     */
    public static final String CODE_TYPE_ALREADY_OPEN = "WORKFLOW_TYPE_ALREADY_OPEN";

    /** User already has an open certification-record workflow for the same definition. */
    public static final String CODE_CERTIFICATION_DEFINITION_ALREADY_OPEN =
            "CERTIFICATION_DEFINITION_ALREADY_OPEN";

    /** User already has an open training-course-record workflow for the same type. */
    public static final String CODE_TRAINING_COURSE_TYPE_ALREADY_OPEN =
            "TRAINING_COURSE_TYPE_ALREADY_OPEN";

    /** User already has an open organization-join request for the same target organization. */
    public static final String CODE_ORGANIZATION_JOIN_ALREADY_OPEN =
            "ORGANIZATION_JOIN_ALREADY_OPEN";

    /**
     * User already has an open main-organization-change request for the same target organization.
     */
    public static final String CODE_MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN =
            "MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN";

    /** User already has an open organization-leave request for the same organization. */
    public static final String CODE_ORGANIZATION_LEAVE_ALREADY_OPEN =
            "ORGANIZATION_LEAVE_ALREADY_OPEN";

    /** User already has an open organization-group-leave request for the same group. */
    public static final String CODE_ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN =
            "ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN";

    /** User already has an open organization-group-join request for the same group. */
    public static final String CODE_ORGANIZATION_GROUP_JOIN_ALREADY_OPEN =
            "ORGANIZATION_GROUP_JOIN_ALREADY_OPEN";

    /** User already has an open workflow for a resolved concurrency key of this type. */
    public static final String CODE_OPEN_CONCURRENCY_KEY_ALREADY_OPEN =
            "OPEN_CONCURRENCY_KEY_ALREADY_OPEN";

    /**
     * Another active workflow already holds a shared exclusivity lock (see {@code
     * WorkflowOpenExclusivityKeys}).
     */
    public static final String CODE_OPEN_EXCLUSIVITY_ALREADY_OPEN = "OPEN_EXCLUSIVITY_ALREADY_OPEN";

    /**
     * Self or org-initiated membership exit already in progress for the same user and organization.
     */
    public static final String CODE_ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN =
            "ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN";

    /**
     * User cannot leave the organization (or have membership ended) while in its leadership group.
     */
    public static final String CODE_LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE =
            "LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE";

    /** User cannot leave the leadership group via a leave request. */
    public static final String CODE_LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED =
            "LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED";

    /** User cannot join the leadership group via a join request. */
    public static final String CODE_LEADERSHIP_GROUP_JOIN_NOT_ALLOWED =
            "LEADERSHIP_GROUP_JOIN_NOT_ALLOWED";

    /** User already has an open onboarding submission for this target organization. */
    public static final String CODE_ONBOARDING_SUBMISSION_ALREADY_OPEN =
            "ONBOARDING_SUBMISSION_ALREADY_OPEN";

    /** An organization-membership end request is already open for this member in this org. */
    public static final String CODE_ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN =
            "ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN";

    /** Membership end would leave the user without an organization anchor. */
    public static final String CODE_USER_ACCOUNT_NOT_SUSTAINABLE = "USER_ACCOUNT_NOT_SUSTAINABLE";

    /** User account is already deactivated; leave / membership-end workflows are not allowed. */
    public static final String CODE_USER_ALREADY_DEACTIVATED = "USER_ALREADY_DEACTIVATED";

    private final String code;

    public WorkflowValidationException(String message) {
        this(message, null);
    }

    public WorkflowValidationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
