/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tenant-scoped picker data for {@code ORG} / {@code ORG_SUBTREE} / {@code ORG_TYPE} audience
 * entries. Only id and display name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAudienceOptionsDto {
    @Builder.Default
    private List<CourseAudienceOrganizationOptionDto> organizations = new ArrayList<>();

    @Builder.Default
    private List<CourseAudienceOrganizationTypeOptionDto> organizationTypes = new ArrayList<>();
}
