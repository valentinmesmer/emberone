/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Training course types an organization may offer: selected in its effective standard and part of
 * the organization's training course catalog (the type must be grantable on completion).
 */
@Service
@RequiredArgsConstructor
public class CourseOfferingCatalogService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationCourseOfferingTypeService courseOfferingTypeService;
    private final TrainingCourseCatalogService trainingCourseCatalogService;

    @Transactional(readOnly = true)
    public List<TrainingCourseTypeDto> listOfferableTypes(Long organizationId) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        Set<String> offerableKeys = courseOfferingTypeService.selectedConfigKeys(organizationId);
        if (offerableKeys.isEmpty()) {
            return List.of();
        }
        return trainingCourseCatalogService
                .getAvailableTypesForOrg(organizationId)
                .getTypes()
                .stream()
                .filter(type -> offerableKeys.contains(type.getConfigKey()))
                .sorted(
                        Comparator.comparing(
                                TrainingCourseTypeDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireOfferable(Long trainingCourseTypeId, Long organizationId) {
        boolean offerable =
                listOfferableTypes(organizationId).stream()
                        .anyMatch(type -> type.getId().equals(trainingCourseTypeId));
        if (!offerable) {
            throw new ConflictException(
                    "Training course type cannot be offered by organization " + organizationId);
        }
    }
}
