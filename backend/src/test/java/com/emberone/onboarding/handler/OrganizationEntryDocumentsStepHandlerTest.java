/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingEntryDocumentDraftSupport;
import com.emberone.onboarding.service.OnboardingEntryDocumentReuseService;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationEntryDocumentsStepHandlerTest {

    @Mock OrganizationOnboardingDocumentDefinitionService organizationDocumentService;
    @Mock OrganizationOnboardingDocumentDefinitionRepository definitionRepository;
    @Mock OnboardingEntryDocumentReuseService documentReuseService;
    @Mock OnboardingEntryDocumentDraftSupport documentDraftSupport;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;

    @InjectMocks OrganizationEntryDocumentsStepHandler handler;

    private static final OnboardingStepFulfillmentContext CONTEXT =
            new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L);

    @Test
    void isFulfilledWhenNoOrgDocumentsConfigured() {
        when(organizationDocumentService.listActiveConfigKeys(99L)).thenReturn(List.of());

        assertThat(handler.isFulfilled(CONTEXT, EmptyStepConfig.INSTANCE)).isTrue();
    }

    @Test
    void isFulfilledWhenAllOrgDocumentsReusable() {
        when(organizationDocumentService.listActiveConfigKeys(99L)).thenReturn(List.of("ORG-DOC"));
        when(documentReuseService.areAllOrganizationReusable(CONTEXT, List.of("ORG-DOC")))
                .thenReturn(true);

        assertThat(handler.isFulfilled(CONTEXT, EmptyStepConfig.INSTANCE)).isTrue();
    }

    @Test
    void validateConfigAcceptsEmptyConfig() {
        handler.validateConfig(1L, EmptyStepConfig.INSTANCE);
    }

    @Test
    void isOnboardingFulfilledWhenOnlyMandatoryRequired() {
        OrganizationOnboardingDocumentDefinition mandatory = definition("MANDATORY", true);
        when(organizationDocumentService.listActiveConfigKeys(99L))
                .thenReturn(List.of("MANDATORY", "OPTIONAL"));
        when(definitionRepository.findByOrganizationIdAndConfigKey(99L, "MANDATORY"))
                .thenReturn(Optional.of(mandatory));
        when(definitionRepository.findByOrganizationIdAndConfigKey(99L, "OPTIONAL"))
                .thenReturn(Optional.of(definition("OPTIONAL", false)));
        when(documentReuseService.findReusableOrganizationConfigKeys(
                        CONTEXT, List.of("MANDATORY", "OPTIONAL")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedOrganizationConfigKeys(
                        CONTEXT, List.of("MANDATORY", "OPTIONAL")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isOrganizationDocumentFulfilled(any(), eq(mandatory)))
                .thenReturn(true);

        assertThat(
                        handler.isOnboardingFulfilled(
                                CONTEXT,
                                EmptyStepConfig.INSTANCE,
                                Map.of(
                                        "documents",
                                        List.of(
                                                Map.of(
                                                        "configKey",
                                                        "MANDATORY",
                                                        "confirmed",
                                                        true,
                                                        "documentVersion",
                                                        1)))))
                .isTrue();
    }

    private static OrganizationOnboardingDocumentDefinition definition(
            String configKey, boolean mandatory) {
        return OrganizationOnboardingDocumentDefinition.builder()
                .tenantId(1L)
                .organizationId(99L)
                .configKey(configKey)
                .name("Doc")
                .description("")
                .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                .mandatory(mandatory)
                .active(true)
                .archived(false)
                .version(1)
                .build();
    }
}
