/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseofferingregistration;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#COURSE_OFFERING_REGISTRATION}. The
 * client sends only {@code offeringId}; everything else is a snapshot set by the server when the
 * workflow opens, so every decider sees the context without further lookups.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOfferingRegistrationWorkflowPayload {

    @NotNull private Long offeringId;

    private Long trainingCourseTypeId;
    private String trainingCourseTypeName;

    /** The offering organization; always decides. */
    private Long offeringOrganizationId;

    private String offeringOrganizationName;

    /**
     * The user's main organization when the offering requires its approval, otherwise {@code null}.
     * Equal to the offering organization means a single decision.
     */
    private Long homeOrganizationId;

    private String homeOrganizationName;

    /** The user already holds the course this offering grants; shown to all deciders. */
    private Boolean alreadyHeld;
}
