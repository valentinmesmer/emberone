/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Minimal organization row for course-offering audience pickers. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAudienceOrganizationOptionDto {
    private Long id;
    private String name;
}
