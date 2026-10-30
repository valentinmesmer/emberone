/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.domain;

import com.emberone.module.domain.Module;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PermissionType {
    ORGANIZATION_READ(1L, Module.CORE),
    ORGANIZATION_GROUP_READ(2L, Module.CORE),
    USER_READ(3L, Module.CORE),
    USER_WRITE(4L, Module.CORE),
    SERVICE_EVENT_READ(5L, Module.SERVICE),
    SERVICE_EVENT_WRITE(6L, Module.SERVICE),
    OPERATION_EVENT_READ(7L, Module.OPERATIONS),
    OPERATION_EVENT_WRITE(8L, Module.OPERATIONS),
    APPOINTMENT_READ(9L, Module.APPOINTMENTS),
    APPOINTMENT_WRITE(10L, Module.APPOINTMENTS),
    EVENT_ATTENDANCE_READ(11L, Module.APPOINTMENTS),
    EVENT_ATTENDANCE_WRITE(12L, Module.APPOINTMENTS),
    EMERGENCY_CONTACT_READ(13L, Module.CORE),
    EMERGENCY_CONTACT_WRITE(14L, Module.CORE),
    ABSENCE_READ(15L, Module.CORE),
    ABSENCE_WRITE(16L, Module.CORE),
    DOCUMENT_READ(17L, Module.CORE),
    DOCUMENT_WRITE(18L, Module.CORE),
    TIME_ENTRY_READ(19L, Module.CORE),
    TIME_ENTRY_WRITE(20L, Module.CORE),
    WORKFLOW_READ(21L, Module.CORE),
    CERTIFICATION_READ(22L, Module.CORE),
    CERTIFICATION_WRITE(23L, Module.CORE),
    TRAINING_COURSE_READ(24L, Module.CORE),
    TRAINING_COURSE_WRITE(25L, Module.CORE),
    QUALIFICATION_READ(26L, Module.CORE),
    QUALIFICATION_WRITE(27L, Module.CORE),
    DRIVING_LICENSE_READ(28L, Module.CORE),
    DRIVING_LICENSE_WRITE(29L, Module.CORE),
    PROFILE_IMAGE_READ(30L, Module.CORE),
    RANK_READ(31L, Module.CORE),
    RANK_WRITE(32L, Module.CORE),
    COURSE_OFFERING_READ(33L, Module.COURSE_OFFERINGS),
    COURSE_OFFERING_WRITE(34L, Module.COURSE_OFFERINGS),
    MANAGEMENT_ACCESS(35L, Module.MANAGEMENT);

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
