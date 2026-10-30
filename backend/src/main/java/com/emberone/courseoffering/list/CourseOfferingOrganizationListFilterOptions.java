/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.service.CourseOfferingService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for {@code GET …/organizations/{id}/course-offerings}. */
@Component
@RequiredArgsConstructor
public class CourseOfferingOrganizationListFilterOptions {

    private final CourseOfferingService offeringService;

    public ListFilterOptionsDto forOrganization(long organizationId) {
        return new ListFilterOptionsDto(
                Map.of(
                        "trainingCourseTypeId",
                        offeringService.listTrainingCourseTypeFilterOptions(organizationId)));
    }
}
