/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.domain;

/**
 * The catalog a workflow type can be split by when routing approvals (tenant-wide routing rules). A
 * handler that returns a dimension from {@code WorkflowHandler#routingDimension()} also resolves
 * the subject ids (ids into that catalog) of an open request.
 */
public enum WorkflowRoutingDimension {
    /** {@code TenantCertDefinition} ids. */
    CERTIFICATION_DEFINITION,
    /** {@code TenantTrainingCourseType} ids. */
    TRAINING_COURSE_TYPE,
    /** {@code TenantDrivingLicenseDefinition} ids. */
    DRIVING_LICENSE_DEFINITION,
    /** {@code TenantRank} ids. */
    RANK
}
