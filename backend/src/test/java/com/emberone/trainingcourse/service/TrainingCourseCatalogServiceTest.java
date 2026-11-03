/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.trainingcourse.standard.TrainingCourseTypeSelection;
import com.emberone.trainingcourse.standard.TrainingCourseTypeSelectionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.userorganizationmembership.service.ActiveOrganizationMembershipLookup;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingCourseCatalogServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 5L;
    private static final long MAIN_ORG_ID = 10L;
    private static final long SECONDARY_ORG_ID = 20L;
    private static final long MAIN_STANDARD_ID = 100L;
    private static final long SECONDARY_STANDARD_ID = 200L;

    @Mock OrganizationRepository organizationRepository;
    @Mock ActiveOrganizationMembershipLookup activeOrganizationMembershipLookup;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock TrainingCourseTypeSelectionRepository selectionRepository;
    @Mock TenantTrainingCourseTypeRepository tenantTypeRepository;

    private TrainingCourseCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService =
                new TrainingCourseCatalogService(
                        organizationRepository,
                        activeOrganizationMembershipLookup,
                        effectiveStandardResolver,
                        selectionRepository,
                        tenantTypeRepository);
    }

    @Test
    void listAvailableTypesForUser_unionsCatalogsAcrossActiveMemberships() {
        Organization mainOrg = Organization.builder().id(MAIN_ORG_ID).tenantId(TENANT_ID).build();
        Organization secondaryOrg =
                Organization.builder().id(SECONDARY_ORG_ID).tenantId(TENANT_ID).build();
        Standard mainStandard = Standard.builder().id(MAIN_STANDARD_ID).tenantId(TENANT_ID).build();
        Standard secondaryStandard =
                Standard.builder().id(SECONDARY_STANDARD_ID).tenantId(TENANT_ID).build();
        TenantTrainingCourseType shared = type(1L, "SHARED", "Truppführer");
        TenantTrainingCourseType secondaryOnly = type(2L, "SECONDARY_ONLY", "Atemschutz");

        when(activeOrganizationMembershipLookup.listActiveOrganizationIds(USER_ID))
                .thenReturn(List.of(MAIN_ORG_ID, SECONDARY_ORG_ID));
        when(organizationRepository.findById(MAIN_ORG_ID)).thenReturn(Optional.of(mainOrg));
        when(organizationRepository.findById(SECONDARY_ORG_ID))
                .thenReturn(Optional.of(secondaryOrg));
        when(effectiveStandardResolver.resolveForOrganization(mainOrg))
                .thenReturn(Optional.of(mainStandard));
        when(effectiveStandardResolver.resolveForOrganization(secondaryOrg))
                .thenReturn(Optional.of(secondaryStandard));
        when(selectionRepository.findByStandardId(MAIN_STANDARD_ID))
                .thenReturn(List.of(selection(MAIN_STANDARD_ID, "SHARED")));
        when(selectionRepository.findByStandardId(SECONDARY_STANDARD_ID))
                .thenReturn(
                        List.of(
                                selection(SECONDARY_STANDARD_ID, "SHARED"),
                                selection(SECONDARY_STANDARD_ID, "SECONDARY_ONLY")));
        when(tenantTypeRepository.findByConfigKeyIn(Set.of("SHARED"))).thenReturn(List.of(shared));
        when(tenantTypeRepository.findByConfigKeyIn(Set.of("SHARED", "SECONDARY_ONLY")))
                .thenReturn(List.of(shared, secondaryOnly));

        assertThat(catalogService.listAvailableTypesForUser(USER_ID))
                .extracting(dto -> dto.getConfigKey())
                .containsExactly("SECONDARY_ONLY", "SHARED");
    }

    @Test
    void requireTypeAvailableForUser_acceptsTypeOnlyInSecondaryMembershipOrg() {
        Organization mainOrg = Organization.builder().id(MAIN_ORG_ID).tenantId(TENANT_ID).build();
        Organization secondaryOrg =
                Organization.builder().id(SECONDARY_ORG_ID).tenantId(TENANT_ID).build();
        Standard secondaryStandard =
                Standard.builder().id(SECONDARY_STANDARD_ID).tenantId(TENANT_ID).build();
        TenantTrainingCourseType secondaryOnly = type(2L, "SECONDARY_ONLY", "Atemschutz");

        when(tenantTypeRepository.findById(2L)).thenReturn(Optional.of(secondaryOnly));
        when(activeOrganizationMembershipLookup.listActiveOrganizationIds(USER_ID))
                .thenReturn(List.of(MAIN_ORG_ID, SECONDARY_ORG_ID));
        when(organizationRepository.findById(MAIN_ORG_ID)).thenReturn(Optional.of(mainOrg));
        when(organizationRepository.findById(SECONDARY_ORG_ID))
                .thenReturn(Optional.of(secondaryOrg));
        when(effectiveStandardResolver.resolveForOrganization(mainOrg))
                .thenReturn(Optional.empty());
        when(effectiveStandardResolver.resolveForOrganization(secondaryOrg))
                .thenReturn(Optional.of(secondaryStandard));
        when(selectionRepository.findByStandardId(SECONDARY_STANDARD_ID))
                .thenReturn(List.of(selection(SECONDARY_STANDARD_ID, "SECONDARY_ONLY")));
        when(tenantTypeRepository.findByConfigKeyIn(Set.of("SECONDARY_ONLY")))
                .thenReturn(List.of(secondaryOnly));

        catalogService.requireTypeAvailableForUser(2L, USER_ID);
    }

    @Test
    void requireTypeAvailableForUser_rejectsWhenMissingFromAllMembershipOrgs() {
        Organization mainOrg = Organization.builder().id(MAIN_ORG_ID).tenantId(TENANT_ID).build();
        TenantTrainingCourseType missing = type(3L, "MISSING", "Missing");

        when(tenantTypeRepository.findById(3L)).thenReturn(Optional.of(missing));
        when(activeOrganizationMembershipLookup.listActiveOrganizationIds(USER_ID))
                .thenReturn(List.of(MAIN_ORG_ID));
        when(organizationRepository.findById(MAIN_ORG_ID)).thenReturn(Optional.of(mainOrg));
        when(effectiveStandardResolver.resolveForOrganization(mainOrg))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.requireTypeAvailableForUser(3L, USER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available for any of the user's organizations");
    }

    private static TenantTrainingCourseType type(long id, String configKey, String name) {
        return TenantTrainingCourseType.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .configKey(configKey)
                .name(name)
                .active(true)
                .archived(false)
                .build();
    }

    private static TrainingCourseTypeSelection selection(long standardId, String configKey) {
        return TrainingCourseTypeSelection.builder()
                .tenantId(TENANT_ID)
                .standardId(standardId)
                .definitionConfigKey(configKey)
                .build();
    }
}
