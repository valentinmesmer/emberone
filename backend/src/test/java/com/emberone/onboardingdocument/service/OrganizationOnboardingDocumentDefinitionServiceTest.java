/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.standard.StandardOnboardingStepRepository;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationOnboardingDocumentDefinitionServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long ORGANIZATION_ID = 2L;
    private static final long STANDARD_ID = 10L;

    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationOnboardingDocumentDefinitionRepository definitionRepository;
    @Mock OrganizationOnboardingDocumentTemplateService templateService;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock StandardOnboardingStepRepository standardOnboardingStepRepository;

    @InjectMocks OrganizationOnboardingDocumentDefinitionService service;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void listRejectsWhenOrganizationEntryDocumentsStepIsDisabled() {
        stubOrganizationAndStandard(false);

        assertThatThrownBy(() -> service.list(TENANT_ID, ORGANIZATION_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Organization entry documents step is not enabled");
    }

    @Test
    void listReturnsDefinitionsWhenOrganizationEntryDocumentsStepIsEnabled() {
        stubOrganizationAndStandard(true);
        when(definitionRepository.findByOrganizationIdOrderByNameAsc(ORGANIZATION_ID))
                .thenReturn(List.of());

        assertThat(service.list(TENANT_ID, ORGANIZATION_ID)).isEmpty();
    }

    @Test
    void isOrganizationEntryDocumentsStepEnabledReflectsEffectiveStandard() {
        stubOrganizationAndStandard(true);

        assertThat(service.isOrganizationEntryDocumentsStepEnabled(TENANT_ID, ORGANIZATION_ID))
                .isTrue();
    }

    private void stubOrganizationAndStandard(boolean stepEnabled) {
        Organization organization =
                Organization.builder().id(ORGANIZATION_ID).tenantId(TENANT_ID).build();
        Standard standard = Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).build();

        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepRepository.existsByStandardIdAndStepType(
                        STANDARD_ID, OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS))
                .thenReturn(stepEnabled);
    }
}
