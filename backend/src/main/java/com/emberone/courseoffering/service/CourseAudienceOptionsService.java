/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.dto.CourseAudienceOptionsDto;
import com.emberone.courseoffering.dto.CourseAudienceOrganizationOptionDto;
import com.emberone.courseoffering.dto.CourseAudienceOrganizationTypeOptionDto;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal tenant organization and organization-type catalogs for course-offering audience editors.
 */
@Service
@RequiredArgsConstructor
public class CourseAudienceOptionsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationTypeDefinitionService organizationTypeDefinitionService;

    @Transactional(readOnly = true)
    public CourseAudienceOptionsDto listForOrganization(Long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        Long tenantId = organization.getTenantId();

        List<CourseAudienceOrganizationOptionDto> organizations =
                organizationRepository.findActiveByTenantIdOrderByName(tenantId).stream()
                        .map(
                                org ->
                                        CourseAudienceOrganizationOptionDto.builder()
                                                .id(org.getId())
                                                .name(org.getName())
                                                .build())
                        .toList();

        List<CourseAudienceOrganizationTypeOptionDto> organizationTypes =
                organizationTypeDefinitionService.listActive().stream()
                        .map(
                                type ->
                                        CourseAudienceOrganizationTypeOptionDto.builder()
                                                .id(type.getId())
                                                .name(type.getName())
                                                .build())
                        .toList();

        return CourseAudienceOptionsDto.builder()
                .organizations(organizations)
                .organizationTypes(organizationTypes)
                .build();
    }
}
