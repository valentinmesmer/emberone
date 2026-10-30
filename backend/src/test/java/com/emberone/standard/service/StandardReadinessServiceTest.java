/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardAssignment;
import com.emberone.standard.domain.StandardKind;
import com.emberone.standard.domain.StandardSource;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.repository.StandardRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandardReadinessServiceTest {

    @Mock StandardAssignmentRepository assignmentRepository;
    @Mock StandardRepository standardRepository;
    @Mock OrganizationTypeDefinitionService organizationTypeDefinitionService;

    @InjectMocks StandardReadinessService service;

    private static Standard systemStandard(long id) {
        return Standard.builder()
                .id(id)
                .tenantId(7L)
                .kind(StandardKind.ORGANIZATION_SETUP)
                .source(StandardSource.SYSTEM)
                .name("System")
                .build();
    }

    private static Standard customStandard(long id) {
        return Standard.builder()
                .id(id)
                .tenantId(7L)
                .kind(StandardKind.ORGANIZATION_SETUP)
                .source(StandardSource.CUSTOM)
                .name("Custom")
                .build();
    }

    private static OrganizationTypeDefinition departmentType() {
        return OrganizationTypeDefinition.builder()
                .id(1L)
                .tenantId(7L)
                .name("Abteilung")
                .active(true)
                .build();
    }

    @Test
    void notReadyWithoutAssignment() {
        when(organizationTypeDefinitionService.listActive()).thenReturn(List.of(departmentType()));
        when(assignmentRepository.findByTenantIdAndOrganizationType_Id(7L, 1L))
                .thenReturn(Optional.empty());

        assertThat(service.isReadyForOrganizationCreation(7L)).isFalse();
    }

    @Test
    void notReadyWhenOnlySystemStandardAssigned() {
        when(organizationTypeDefinitionService.listActive()).thenReturn(List.of(departmentType()));
        when(assignmentRepository.findByTenantIdAndOrganizationType_Id(7L, 1L))
                .thenReturn(
                        Optional.of(
                                StandardAssignment.builder()
                                        .tenantId(7L)
                                        .organizationType(departmentType())
                                        .standardId(1L)
                                        .build()));
        when(standardRepository.findById(1L)).thenReturn(Optional.of(systemStandard(1L)));

        assertThat(service.isReadyForOrganizationCreation(7L)).isFalse();
    }

    @Test
    void readyWhenCustomStandardAssigned() {
        when(organizationTypeDefinitionService.listActive()).thenReturn(List.of(departmentType()));
        when(assignmentRepository.findByTenantIdAndOrganizationType_Id(7L, 1L))
                .thenReturn(
                        Optional.of(
                                StandardAssignment.builder()
                                        .tenantId(7L)
                                        .organizationType(departmentType())
                                        .standardId(2L)
                                        .build()));
        when(standardRepository.findById(2L)).thenReturn(Optional.of(customStandard(2L)));

        assertThat(service.isReadyForOrganizationCreation(7L)).isTrue();
    }
}
