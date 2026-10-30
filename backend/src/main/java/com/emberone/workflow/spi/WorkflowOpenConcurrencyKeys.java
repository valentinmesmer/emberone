/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

import java.util.Optional;

/** Stable key format for per-target open workflow concurrency within a workflow type. */
public final class WorkflowOpenConcurrencyKeys {

    private static final String ORGANIZATION_PREFIX = "org:";
    private static final String ORGANIZATION_GROUP_PREFIX = "group:";
    private static final String CERTIFICATION_DEFINITION_PREFIX = "cert:";
    private static final String TRAINING_COURSE_TYPE_PREFIX = "training:";
    private static final String USER_PREFIX = "user:";
    private static final String COURSE_OFFERING_PREFIX = "course-offering:";
    private static final String COURSE_REGISTRATION_PREFIX = "course-registration:";

    private WorkflowOpenConcurrencyKeys() {}

    public static String organization(long organizationId) {
        return ORGANIZATION_PREFIX + organizationId;
    }

    public static String organizationGroup(long organizationGroupId) {
        return ORGANIZATION_GROUP_PREFIX + organizationGroupId;
    }

    public static String certificationDefinition(long tenantCertDefinitionId) {
        return CERTIFICATION_DEFINITION_PREFIX + tenantCertDefinitionId;
    }

    public static String trainingCourseType(long tenantTrainingCourseTypeId) {
        return TRAINING_COURSE_TYPE_PREFIX + tenantTrainingCourseTypeId;
    }

    public static String courseOffering(long offeringId) {
        return COURSE_OFFERING_PREFIX + offeringId;
    }

    public static String courseRegistration(long registrationId) {
        return COURSE_REGISTRATION_PREFIX + registrationId;
    }

    public static String user(long userId) {
        return USER_PREFIX + userId;
    }

    public static Optional<Long> parseOrganizationId(String key) {
        return parseSuffix(key, ORGANIZATION_PREFIX);
    }

    public static Optional<Long> parseOrganizationGroupId(String key) {
        return parseSuffix(key, ORGANIZATION_GROUP_PREFIX);
    }

    private static Optional<Long> parseSuffix(String key, String prefix) {
        if (key == null || !key.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(key.substring(prefix.length())));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
