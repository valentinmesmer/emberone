/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingEntryDocumentDraftSupportTest {

    @Mock TenantOnboardingDocumentDefinitionRepository tenantDefinitionRepository;
    @Mock OrganizationOnboardingDocumentDefinitionRepository orgDefinitionRepository;
    @Mock OnboardingDocumentTemplateService tenantTemplateService;
    @Mock OrganizationOnboardingDocumentTemplateService organizationTemplateService;

    @InjectMocks OnboardingEntryDocumentDraftSupport support;

    @Test
    void stampTenantDocumentVersionsStampsUploadWhenStorageKeyPresent() {
        TenantOnboardingDocumentDefinition definition =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(1L)
                        .configKey("DSGVO")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD)
                        .archived(false)
                        .version(1)
                        .build();
        when(tenantDefinitionRepository.findByConfigKey("DSGVO"))
                .thenReturn(Optional.of(definition));

        Map<String, Object> stamped =
                support.stampTenantDocumentVersions(
                        Map.of(
                                "documents",
                                List.of(
                                        Map.of(
                                                "configKey",
                                                "DSGVO",
                                                "confirmed",
                                                false,
                                                "storageKey",
                                                "tenants/1/upload.pdf"))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) stamped.get("documents");
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).containsEntry("documentVersion", 1);
    }

    @Test
    void isTenantDocumentFulfilledIgnoresOutdatedVersion() {
        TenantOnboardingDocumentDefinition definition =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(1L)
                        .configKey("DOC")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .archived(false)
                        .version(3)
                        .build();

        boolean fulfilled =
                support.isTenantDocumentFulfilled(
                        Map.of("configKey", "DOC", "confirmed", true, "documentVersion", 2),
                        definition);

        assertThat(fulfilled).isFalse();
    }

    @Test
    void isTenantDocumentDeclinedRequiresMatchingVersion() {
        TenantOnboardingDocumentDefinition definition =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(1L)
                        .configKey("OPTIONAL")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .mandatory(false)
                        .active(true)
                        .archived(false)
                        .version(2)
                        .build();

        assertThat(
                        support.isTenantDocumentDeclined(
                                Map.of(
                                        "configKey",
                                        "OPTIONAL",
                                        "declined",
                                        true,
                                        "documentVersion",
                                        2),
                                definition))
                .isTrue();
    }

    @Test
    void finalizeRejectsConfirmWithoutOpenedTemplate() {
        TenantOnboardingDocumentDefinition definition =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(1L)
                        .configKey("DOC")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .mandatory(true)
                        .active(true)
                        .archived(false)
                        .version(1)
                        .build();
        when(tenantDefinitionRepository.findByConfigKey("DOC")).thenReturn(Optional.of(definition));
        when(tenantTemplateService.hasCurrentTemplate(definition)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                support.finalizeTenantEntryDocumentPayload(
                                        Map.of(
                                                "documents",
                                                List.of(
                                                        Map.of(
                                                                "configKey",
                                                                "DOC",
                                                                "confirmed",
                                                                true,
                                                                "confirmationText",
                                                                "I accept",
                                                                "documentVersion",
                                                                1))),
                                        Map.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("opened");
    }

    @Test
    void finalizeAcceptsConfirmAfterTemplateOpened() {
        TenantOnboardingDocumentDefinition definition =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(1L)
                        .configKey("DOC")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .mandatory(true)
                        .active(true)
                        .archived(false)
                        .version(1)
                        .build();
        when(tenantDefinitionRepository.findByConfigKey("DOC")).thenReturn(Optional.of(definition));
        when(tenantTemplateService.hasCurrentTemplate(definition)).thenReturn(true);

        Map<String, Object> finalized =
                support.finalizeTenantEntryDocumentPayload(
                        Map.of(
                                "documents",
                                List.of(
                                        Map.of(
                                                "configKey",
                                                "DOC",
                                                "confirmed",
                                                true,
                                                "confirmationText",
                                                "I accept",
                                                "documentVersion",
                                                1,
                                                "openedAt",
                                                Instant.parse("2024-01-01T00:00:00Z").toString()))),
                        Map.of());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) finalized.get("documents");
        assertThat(documents.get(0)).containsKey("confirmedAt");
    }
}
