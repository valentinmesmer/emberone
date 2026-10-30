/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.dto.CourseAudienceOptionsDto;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAudienceOptionsServiceTest {

    private static final long ORG_ID = 10L;
    private static final long TENANT_ID = 7L;

    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationTypeDefinitionService organizationTypeDefinitionService;

    @InjectMocks CourseAudienceOptionsService service;

    @Test
    void listsActiveOrganizationsAndTypesForTenant() {
        when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.of(org(ORG_ID, "Caller")));
        when(organizationRepository.findActiveByTenantIdOrderByName(TENANT_ID))
                .thenReturn(List.of(org(2L, "Child"), org(1L, "Root")));
        when(organizationTypeDefinitionService.listActive())
                .thenReturn(
                        List.of(
                                OrganizationTypeDefinition.builder()
                                        .id(5L)
                                        .tenantId(TENANT_ID)
                                        .name("Feuerwehr")
                                        .active(true)
                                        .build()));

        CourseAudienceOptionsDto result = service.listForOrganization(ORG_ID);

        assertThat(result.getOrganizations())
                .extracting(option -> option.getId() + ":" + option.getName())
                .containsExactly("2:Child", "1:Root");
        assertThat(result.getOrganizationTypes())
                .extracting(option -> option.getId() + ":" + option.getName())
                .containsExactly("5:Feuerwehr");
    }

    @Test
    void unknownOrganizationIsNotFound() {
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForOrganization(ORG_ID))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    private static Organization org(long id, String name) {
        return Organization.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .organizationCode("C" + id)
                .name(name)
                .build();
    }
}
