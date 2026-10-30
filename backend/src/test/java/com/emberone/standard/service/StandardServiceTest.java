/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.certification.standard.CertificationStandardDomain;
import com.emberone.drivinglicense.standard.DrivingLicenseStandardDomain;
import com.emberone.onboarding.service.StandardOnboardingStepService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.organizationgroup.standard.OrganizationGroupStandardDomain;
import com.emberone.permissiongroup.standard.PermissionGroupStandardDomain;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.qualification.standard.QualificationStandardDomain;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardKind;
import com.emberone.standard.domain.StandardSource;
import com.emberone.standard.event.OrganizationStandardOverrideChangedEvent;
import com.emberone.standard.event.StandardEnabledModulesChangedEvent;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.repository.StandardModuleRepository;
import com.emberone.standard.repository.StandardRepository;
import com.emberone.standard.spi.StandardDomainRegistry;
import com.emberone.trainingcourse.standard.TrainingCourseStandardDomain;
import com.emberone.workflow.standard.WorkflowTypeMappingStandardDomain;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandardServiceTest {

    private static final OrganizationTypeDefinition DEPARTMENT =
            OrganizationTypeDefinition.builder()
                    .id(1L)
                    .tenantId(10L)
                    .name("Abteilung")
                    .active(true)
                    .build();

    @Mock StandardRepository standardRepository;
    @Mock StandardAssignmentRepository assignmentRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock StandardDomainRegistry registry;
    @Mock StandardMaterializationService materializationService;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock OrganizationGroupStandardDomain organizationGroupStandardDomain;
    @Mock PermissionGroupStandardDomain permissionGroupStandardDomain;
    @Mock WorkflowTypeMappingStandardDomain workflowTypeMappingStandardDomain;
    @Mock CertificationStandardDomain certificationStandardDomain;
    @Mock TrainingCourseStandardDomain trainingCourseStandardDomain;

    @Mock DrivingLicenseStandardDomain drivingLicenseStandardDomain;
    @Mock QualificationStandardDomain qualificationStandardDomain;
    @Mock com.emberone.rank.standard.RankStandardDomain rankStandardDomain;

    @Mock
    com.emberone.dutyfunction.standard.FunctionSelectionStandardDomain
            functionSelectionStandardDomain;

    @Mock com.emberone.dutyfunction.standard.DutyFunctionStandardDomain dutyFunctionStandardDomain;
    @Mock StandardOnboardingStepService standardOnboardingStepService;
    @Mock TenantBaselineSyncService tenantBaselineSyncService;
    @Mock OrganizationTypeDefinitionService organizationTypeDefinitionService;
    @Mock StandardModuleRepository standardModuleRepository;
    @Mock EnabledModuleResolver enabledModuleResolver;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

    private StandardService standardService;

    @BeforeEach
    void setUp() {
        standardService =
                new StandardService(
                        standardRepository,
                        assignmentRepository,
                        organizationRepository,
                        registry,
                        materializationService,
                        effectiveStandardResolver,
                        organizationGroupStandardDomain,
                        permissionGroupStandardDomain,
                        workflowTypeMappingStandardDomain,
                        certificationStandardDomain,
                        trainingCourseStandardDomain,
                        drivingLicenseStandardDomain,
                        qualificationStandardDomain,
                        rankStandardDomain,
                        functionSelectionStandardDomain,
                        dutyFunctionStandardDomain,
                        standardOnboardingStepService,
                        tenantBaselineSyncService,
                        organizationTypeDefinitionService,
                        standardModuleRepository,
                        enabledModuleResolver,
                        eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getStandardRejectsForeignTenantAtApplicationLevel() {
        TenantContextHolder.setTenantId(10L);
        Standard foreign = Standard.builder().id(2L).tenantId(99L).name("Other tenant").build();
        when(standardRepository.findById(2L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> standardService.getStandard(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Standard not found");
    }

    @Test
    void assignStandardRejectsSystemStandard() {
        TenantContextHolder.setTenantId(10L);
        Standard systemStandard =
                Standard.builder()
                        .id(1L)
                        .tenantId(10L)
                        .kind(StandardKind.ORGANIZATION_SETUP)
                        .source(StandardSource.SYSTEM)
                        .name("EmberOne-Standard")
                        .build();
        when(standardRepository.findById(1L)).thenReturn(Optional.of(systemStandard));

        assertThatThrownBy(() -> standardService.assignStandard(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be assigned");
    }

    @Test
    void setOrganizationStandardOverrideAssignsCustomStandardAndReassigns() {
        TenantContextHolder.setTenantId(10L);
        Organization org = Organization.builder().id(5L).tenantId(10L).type(DEPARTMENT).build();
        Standard custom =
                Standard.builder()
                        .id(3L)
                        .tenantId(10L)
                        .source(StandardSource.CUSTOM)
                        .name("Custom")
                        .build();
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org));
        when(standardRepository.findById(3L)).thenReturn(Optional.of(custom));
        when(organizationRepository.save(org)).thenReturn(org);

        standardService.setOrganizationStandardOverride(5L, 3L);

        assertThat(org.getStandardId()).isEqualTo(3L);
        verify(materializationService).reassignStandardForOrg(org, custom);
        verify(eventPublisher)
                .publishEvent(new OrganizationStandardOverrideChangedEvent(10L, 5L, null, 3L));
    }

    @Test
    void setOrganizationStandardOverrideRejectsSystemStandard() {
        TenantContextHolder.setTenantId(10L);
        Organization org = Organization.builder().id(5L).tenantId(10L).type(DEPARTMENT).build();
        Standard systemStandard =
                Standard.builder()
                        .id(1L)
                        .tenantId(10L)
                        .source(StandardSource.SYSTEM)
                        .name("EmberOne-Standard")
                        .build();
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org));
        when(standardRepository.findById(1L)).thenReturn(Optional.of(systemStandard));

        assertThatThrownBy(() -> standardService.setOrganizationStandardOverride(5L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be assigned");
        verify(materializationService, never()).reassignStandardForOrg(any(), any());
    }

    @Test
    void clearingOrganizationStandardOverrideResetsToInheritAndReassigns() {
        TenantContextHolder.setTenantId(10L);
        Organization org =
                Organization.builder().id(5L).tenantId(10L).standardId(3L).type(DEPARTMENT).build();
        Standard typeDefault =
                Standard.builder()
                        .id(2L)
                        .tenantId(10L)
                        .source(StandardSource.CUSTOM)
                        .name("Type Default")
                        .build();
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org));
        when(organizationRepository.save(org)).thenReturn(org);
        when(effectiveStandardResolver.resolveForOrganizationType(1L))
                .thenReturn(Optional.of(typeDefault));

        standardService.setOrganizationStandardOverride(5L, null);

        assertThat(org.getStandardId()).isNull();
        verify(materializationService).reassignStandardForOrg(org, typeDefault);
        verify(eventPublisher)
                .publishEvent(new OrganizationStandardOverrideChangedEvent(10L, 5L, 3L, null));
    }

    @Test
    void deleteClearsCloneLineageThenRemovesStandard() {
        TenantContextHolder.setTenantId(10L);
        Standard custom =
                Standard.builder()
                        .id(3L)
                        .tenantId(10L)
                        .source(StandardSource.CUSTOM)
                        .name("Main")
                        .build();
        when(standardRepository.findById(3L)).thenReturn(Optional.of(custom));
        when(assignmentRepository.existsByStandardId(3L)).thenReturn(false);
        when(organizationRepository.existsByStandardId(3L)).thenReturn(false);
        when(standardRepository.detachCloneLineage(3L)).thenReturn(1);

        standardService.deleteStandard(3L);

        verify(standardRepository).detachCloneLineage(3L);
        verify(standardRepository).delete(custom);
        verify(eventPublisher).publishEvent(new StandardEnabledModulesChangedEvent(10L, 3L));
    }

    @Test
    void cloneStandardSelectsAllCatalogItems() {
        TenantContextHolder.setTenantId(10L);
        Standard source =
                Standard.builder()
                        .id(1L)
                        .tenantId(10L)
                        .kind(StandardKind.ORGANIZATION_SETUP)
                        .source(StandardSource.SYSTEM)
                        .name("System")
                        .build();
        Standard saved =
                Standard.builder()
                        .id(2L)
                        .tenantId(10L)
                        .kind(StandardKind.ORGANIZATION_SETUP)
                        .source(StandardSource.CUSTOM)
                        .name("Copy")
                        .baseStandardId(1L)
                        .build();
        when(standardRepository.findById(1L)).thenReturn(Optional.of(source));
        when(standardRepository.existsByNameIgnoreCase("Copy")).thenReturn(false);
        when(standardRepository.save(any(Standard.class))).thenReturn(saved);
        when(standardModuleRepository.findAllByTenantIdAndStandardId(10L, 1L))
                .thenReturn(List.of());

        Standard result = standardService.cloneStandard(1L, "Copy", null);

        assertThat(result.getId()).isEqualTo(2L);
        verify(registry).copyTemplates(source, saved);
        verify(standardOnboardingStepService).copySteps(source, saved);
        verify(certificationStandardDomain).selectAllActiveDefinitions(saved);
        verify(trainingCourseStandardDomain).selectAllActiveDefinitions(saved);
        verify(drivingLicenseStandardDomain).selectAllActiveDefinitions(saved);
        verify(qualificationStandardDomain).selectAllActiveDefinitions(saved);
        verify(organizationGroupStandardDomain).ensureRequiredSystemGroups(saved);
    }

    @Test
    void deleteRejectsStandardStillUsedAsOrgOverride() {
        TenantContextHolder.setTenantId(10L);
        Standard custom =
                Standard.builder()
                        .id(3L)
                        .tenantId(10L)
                        .source(StandardSource.CUSTOM)
                        .name("Custom")
                        .build();
        when(standardRepository.findById(3L)).thenReturn(Optional.of(custom));
        when(assignmentRepository.existsByStandardId(3L)).thenReturn(false);
        when(organizationRepository.existsByStandardId(3L)).thenReturn(true);

        assertThatThrownBy(() -> standardService.deleteStandard(3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("override");
        verify(standardRepository, never()).delete(any());
    }

    @Test
    void updateWorkflowRejectsAllMembersApproverGroup() {
        TenantContextHolder.setTenantId(10L);
        Standard custom =
                Standard.builder()
                        .id(3L)
                        .tenantId(10L)
                        .source(StandardSource.CUSTOM)
                        .name("Custom")
                        .build();
        when(standardRepository.findById(3L)).thenReturn(Optional.of(custom));
        doThrow(
                        new ConflictException(
                                "Organization group 'ALL_MEMBERS' cannot be used as a workflow approver"))
                .when(organizationGroupStandardDomain)
                .requireWorkflowApproverOrgGroup(custom, "ALL_MEMBERS");

        assertThatThrownBy(
                        () ->
                                standardService.updateWorkflow(
                                        3L, "1", "ALL_MEMBERS", false, true, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be used as a workflow approver");
        verify(workflowTypeMappingStandardDomain, never())
                .updateMappingTemplate(
                        eq(custom),
                        anyString(),
                        anyString(),
                        anyBoolean(),
                        anyBoolean(),
                        anyBoolean(),
                        any());
    }
}
