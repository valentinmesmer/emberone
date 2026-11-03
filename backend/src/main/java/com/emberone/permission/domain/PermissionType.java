/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permission.domain;

import com.emberone.module.domain.Module;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PermissionType {
    ORGANIZATION_GROUP_READ(1L, Module.CORE),
    USER_READ(2L, Module.CORE),
    USER_WRITE(3L, Module.CORE),
    SERVICE_EVENT_READ(4L, Module.SERVICE),
    SERVICE_EVENT_WRITE(5L, Module.SERVICE),
    OPERATION_EVENT_READ(6L, Module.OPERATIONS),
    OPERATION_EVENT_WRITE(7L, Module.OPERATIONS),
    APPOINTMENT_READ(8L, Module.APPOINTMENTS),
    APPOINTMENT_WRITE(9L, Module.APPOINTMENTS),
    EVENT_ATTENDANCE_READ(10L, Module.APPOINTMENTS),
    EVENT_ATTENDANCE_WRITE(11L, Module.APPOINTMENTS),
    EMERGENCY_CONTACT_READ(12L, Module.CORE),
    EMERGENCY_CONTACT_WRITE(13L, Module.CORE),
    ABSENCE_READ(14L, Module.CORE),
    ABSENCE_WRITE(15L, Module.CORE),
    DOCUMENT_READ(16L, Module.CORE),
    DOCUMENT_WRITE(17L, Module.CORE),
    TIME_ENTRY_READ(18L, Module.CORE),
    TIME_ENTRY_WRITE(19L, Module.CORE),
    WORKFLOW_READ(20L, Module.CORE),
    CERTIFICATION_READ(21L, Module.CORE),
    CERTIFICATION_WRITE(22L, Module.CORE),
    TRAINING_COURSE_READ(23L, Module.CORE),
    TRAINING_COURSE_WRITE(24L, Module.CORE),
    QUALIFICATION_READ(25L, Module.CORE),
    QUALIFICATION_WRITE(26L, Module.CORE),
    DRIVING_LICENSE_READ(27L, Module.CORE),
    DRIVING_LICENSE_WRITE(28L, Module.CORE),
    PROFILE_IMAGE_READ(29L, Module.CORE),
    RANK_READ(30L, Module.CORE),
    RANK_WRITE(31L, Module.CORE),
    COURSE_OFFERING_READ(32L, Module.COURSE_OFFERINGS),
    COURSE_OFFERING_WRITE(33L, Module.COURSE_OFFERINGS),
    MANAGEMENT_ACCESS(34L, Module.MANAGEMENT);

    private final Long id;
    private final Module module;

    @JsonCreator
    public static PermissionType fromId(Number id) {
        if (id == null) {
            return null;
        }
        long lid = id.longValue();
        for (PermissionType type : values()) {
            if (type.getId() == lid) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PermissionType id: " + id);
    }

    @JsonValue
    public Long getId() {
        return id;
    }

    public Module getModule() {
        return module;
    }
}
