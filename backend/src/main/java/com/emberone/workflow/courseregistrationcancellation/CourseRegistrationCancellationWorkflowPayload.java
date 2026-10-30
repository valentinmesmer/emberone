/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseregistrationcancellation;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#COURSE_REGISTRATION_CANCELLATION}.
 * The client sends only {@code registrationId}; everything else is a snapshot set by the server
 * when the workflow opens, so both deciding organizations see the context without further lookups.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRegistrationCancellationWorkflowPayload {

    @NotNull private Long registrationId;

    private Long offeringId;
    private String trainingCourseTypeName;
    private Instant firstSessionStart;

    /** The offering organization; always decides. */
    private Long offeringOrganizationId;

    private String offeringOrganizationName;

    /**
     * The user's main organization; always decides as well. Equal to the offering organization
     * means a single decision.
     */
    private Long homeOrganizationId;

    private String homeOrganizationName;
}
