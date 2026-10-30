/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Course types an organization may offer. {@code enabled} is false when the course offering module
 * is off for the organization's standard; the selection is then neither shown nor writable.
 */
@Value
@Builder
public class CourseOfferingTypeSettingsDto {

    boolean enabled;
    List<String> definitionConfigKeys;
}
