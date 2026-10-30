/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.qualification.standard.QualificationSelection;
import com.emberone.qualification.standard.QualificationSelectionRepository;
import com.emberone.qualification.tenant.TenantQualificationDefinition;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QualificationCatalogServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long STANDARD_ID = 20L;

    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock QualificationSelectionRepository selectionRepository;
    @Mock TenantQualificationDefinitionRepository definitionRepository;

    private QualificationCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService =
                new QualificationCatalogService(
                        organizationRepository,
                        effectiveStandardResolver,
                        selectionRepository,
                        definitionRepository);
    }

    @Test
    void resolveCatalogForOrganization_returnsSelectedActiveDefinitions() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        Standard standard = Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).build();
        TenantQualificationDefinition definition =
                TenantQualificationDefinition.builder()
                        .id(100L)
                        .tenantId(TENANT_ID)
                        .configKey("MASCHINIST")
                        .name("Maschinist")
                        .active(true)
                        .archived(false)
                        .requiresOrgDecision(true)
                        .build();

        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(selectionRepository.findByStandardId(STANDARD_ID))
                .thenReturn(
                        List.of(
                                QualificationSelection.builder()
                                        .tenantId(TENANT_ID)
                                        .standardId(STANDARD_ID)
                                        .definitionConfigKey("MASCHINIST")
                                        .build()));
        when(definitionRepository.findByConfigKeyIn(List.of("MASCHINIST")))
                .thenReturn(List.of(definition));

        var catalog = catalogService.resolveCatalogForOrganization(ORG_ID);

        assertThat(catalog).hasSize(1);
        assertThat(catalog.getFirst().getConfigKey()).isEqualTo("MASCHINIST");
        assertThat(catalog.getFirst().isRequiresOrgDecision()).isTrue();
    }

    @Test
    void resolveCatalogForOrganization_returnsEmptyWhenNoStandard() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.empty());

        assertThat(catalogService.resolveCatalogForOrganization(ORG_ID)).isEmpty();
    }
}
