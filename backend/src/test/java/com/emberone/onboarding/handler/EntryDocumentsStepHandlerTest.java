/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.config.EntryDocumentsStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingEntryDocumentDraftSupport;
import com.emberone.onboarding.service.OnboardingEntryDocumentReuseService;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
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
class EntryDocumentsStepHandlerTest {

    @Mock TenantOnboardingDocumentDefinitionRepository definitionRepository;
    @Mock OnboardingDocumentTemplateService documentTemplateService;
    @Mock OnboardingEntryDocumentReuseService documentReuseService;
    @Mock OnboardingEntryDocumentDraftSupport documentDraftSupport;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;

    @InjectMocks EntryDocumentsStepHandler handler;

    private static final OnboardingStepFulfillmentContext CONTEXT =
            new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L);

    @Test
    void validateConfigRejectsEmptySelection() {
        assertThatThrownBy(
                        () -> handler.validateConfig(1L, new EntryDocumentsStepConfig(List.of())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validateConfigAcceptsActiveDocumentWithTemplate() {
        TenantOnboardingDocumentDefinition definition =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(1L)
                        .configKey("DOC")
                        .name("Doc")
                        .description("")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .mandatory(true)
                        .active(true)
                        .archived(false)
                        .version(1)
                        .build();
        when(definitionRepository.findByConfigKey("DOC")).thenReturn(Optional.of(definition));
        when(documentTemplateService.hasCurrentTemplate(definition)).thenReturn(true);

        handler.validateConfig(1L, new EntryDocumentsStepConfig(List.of("DOC")));

        assertThat(handler.toConfigMap(new EntryDocumentsStepConfig(List.of("DOC"))))
                .containsEntry("documentConfigKeys", List.of("DOC"));
    }

    @Test
    void isFulfilledWhenAllDocumentsInUserTable() {
        when(documentReuseService.areAllTenantReusable(eq(CONTEXT), eq(List.of("DOC"))))
                .thenReturn(true);

        assertThat(handler.isFulfilled(CONTEXT, new EntryDocumentsStepConfig(List.of("DOC"))))
                .isTrue();
    }

    @Test
    void hasInvitationWorkWhenDraftConfirmed() {
        assertThat(
                        handler.hasInvitationWork(
                                new EntryDocumentsStepConfig(List.of("DOC")),
                                Map.of(
                                        "documents",
                                        List.of(Map.of("configKey", "DOC", "confirmed", true)))))
                .isTrue();
    }

    @Test
    void hasInvitationWorkWhenSignedUploadPresent() {
        assertThat(
                        handler.hasInvitationWork(
                                new EntryDocumentsStepConfig(List.of("DOC")),
                                Map.of(
                                        "documents",
                                        List.of(
                                                Map.of(
                                                        "configKey",
                                                        "DOC",
                                                        "storageKey",
                                                        "tenants/1/doc.pdf")))))
                .isTrue();
    }

    @Test
    void hasNoInvitationWorkWhenOnlyLiveReuse() {
        assertThat(
                        handler.hasInvitationWork(
                                new EntryDocumentsStepConfig(List.of("DOC")),
                                Map.of("documents", List.of())))
                .isFalse();
    }

    @Test
    void isOnboardingFulfilledWhenConfirmedInDraft() {
        TenantOnboardingDocumentDefinition definition = definition("DOC");
        when(definitionRepository.findByConfigKey("DOC")).thenReturn(Optional.of(definition));
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isTenantDocumentFulfilled(any(), eq(definition)))
                .thenReturn(true);

        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        CONTEXT,
                        new EntryDocumentsStepConfig(List.of("DOC")),
                        Map.of(
                                "documents",
                                List.of(
                                        Map.of(
                                                "configKey",
                                                "DOC",
                                                "confirmed",
                                                true,
                                                "documentVersion",
                                                1))));

        assertThat(fulfilled).isTrue();
    }

    @Test
    void isOnboardingFulfilledWhenReusedFromUserTable() {
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of("DOC"));
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());

        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        CONTEXT,
                        new EntryDocumentsStepConfig(List.of("DOC")),
                        Map.of("documents", List.of()));

        assertThat(fulfilled).isTrue();
    }

    @Test
    void isOnboardingFulfilledWhenOnlyMandatoryRequiredAndOptionalOpen() {
        TenantOnboardingDocumentDefinition mandatory = definition("MANDATORY");
        TenantOnboardingDocumentDefinition optional =
                definition("OPTIONAL").toBuilder().mandatory(false).build();
        when(definitionRepository.findByConfigKey("MANDATORY")).thenReturn(Optional.of(mandatory));
        when(definitionRepository.findByConfigKey("OPTIONAL")).thenReturn(Optional.of(optional));
        when(documentReuseService.findReusableTenantConfigKeys(
                        CONTEXT, List.of("MANDATORY", "OPTIONAL")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(
                        CONTEXT, List.of("MANDATORY", "OPTIONAL")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isTenantDocumentFulfilled(any(), eq(mandatory))).thenReturn(true);

        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        CONTEXT,
                        new EntryDocumentsStepConfig(List.of("MANDATORY", "OPTIONAL")),
                        Map.of(
                                "documents",
                                List.of(
                                        Map.of(
                                                "configKey",
                                                "MANDATORY",
                                                "confirmed",
                                                true,
                                                "documentVersion",
                                                1))));

        assertThat(fulfilled).isTrue();
    }

    @Test
    void isOnboardingFulfilledWhenOptionalDeclinedInDraft() {
        TenantOnboardingDocumentDefinition optional =
                definition("OPTIONAL").toBuilder().mandatory(false).build();
        when(definitionRepository.findByConfigKey("OPTIONAL")).thenReturn(Optional.of(optional));
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("OPTIONAL")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("OPTIONAL")))
                .thenReturn(Set.of());

        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        CONTEXT,
                        new EntryDocumentsStepConfig(List.of("OPTIONAL")),
                        Map.of(
                                "documents",
                                List.of(
                                        Map.of(
                                                "configKey",
                                                "OPTIONAL",
                                                "declined",
                                                true,
                                                "documentVersion",
                                                1))));

        assertThat(fulfilled).isTrue();
    }

    @Test
    void isOnboardingNotFulfilledWhenMandatoryMissing() {
        TenantOnboardingDocumentDefinition mandatory = definition("MANDATORY");
        when(definitionRepository.findByConfigKey("MANDATORY")).thenReturn(Optional.of(mandatory));
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("MANDATORY")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("MANDATORY")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isTenantDocumentFulfilled(any(), eq(mandatory)))
                .thenReturn(false);

        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        CONTEXT,
                        new EntryDocumentsStepConfig(List.of("MANDATORY")),
                        Map.of("documents", List.of()));

        assertThat(fulfilled).isFalse();
    }

    @Test
    void isOnboardingNotFulfilledWhenUnsignedUploadMissing() {
        TenantOnboardingDocumentDefinition definition =
                definition("DOC").toBuilder()
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD)
                        .build();
        when(definitionRepository.findByConfigKey("DOC")).thenReturn(Optional.of(definition));
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isTenantDocumentFulfilled(any(), eq(definition)))
                .thenReturn(false);

        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        CONTEXT,
                        new EntryDocumentsStepConfig(List.of("DOC")),
                        Map.of(
                                "documents",
                                List.of(Map.of("configKey", "DOC", "confirmed", true))));

        assertThat(fulfilled).isFalse();
    }

    @Test
    void promoteSkipsWhenAlreadyLiveFulfilled() {
        when(documentReuseService.areAllTenantReusable(eq(CONTEXT), eq(List.of("DOC"))))
                .thenReturn(true);

        handler.promote(
                promotionContext(),
                new EntryDocumentsStepConfig(List.of("DOC")),
                Map.of("documents", List.of()));

        verify(attachmentPromotionService, never())
                .upsertConfirmedEntryDocument(any(), any(), any());
    }

    @Test
    void promoteRunsWhenMandatoryLiveFulfilledButOptionalAcceptedInDraft() {
        TenantOnboardingDocumentDefinition optional =
                definition("OPTIONAL").toBuilder().mandatory(false).build();
        when(documentReuseService.areAllTenantReusable(eq(CONTEXT), eq(List.of("OPTIONAL"))))
                .thenReturn(true);
        when(definitionRepository.findByConfigKey("OPTIONAL")).thenReturn(Optional.of(optional));
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("OPTIONAL")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("OPTIONAL")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isTenantDocumentFulfilled(any(), eq(optional))).thenReturn(true);

        handler.promote(
                promotionContext(),
                new EntryDocumentsStepConfig(List.of("OPTIONAL")),
                Map.of(
                        "documents",
                        List.of(
                                Map.of(
                                        "configKey",
                                        "OPTIONAL",
                                        "confirmed",
                                        true,
                                        "documentVersion",
                                        1))));

        verify(attachmentPromotionService)
                .upsertConfirmedEntryDocument(any(), eq("OPTIONAL"), any());
    }

    @Test
    void promoteUpsertsConfirmedEntryDocumentFromDraft() {
        TenantOnboardingDocumentDefinition definition = definition("DOC");
        when(definitionRepository.findByConfigKey("DOC")).thenReturn(Optional.of(definition));
        when(documentReuseService.findReusableTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());
        when(documentReuseService.findDeclinedTenantConfigKeys(CONTEXT, List.of("DOC")))
                .thenReturn(Set.of());
        when(documentDraftSupport.isTenantDocumentFulfilled(any(), eq(definition)))
                .thenReturn(true);

        handler.promote(
                promotionContext(),
                new EntryDocumentsStepConfig(List.of("DOC")),
                Map.of(
                        "documents",
                        List.of(
                                Map.of(
                                        "configKey",
                                        "DOC",
                                        "confirmed",
                                        true,
                                        "documentVersion",
                                        1))));

        verify(attachmentPromotionService).upsertConfirmedEntryDocument(any(), eq("DOC"), any());
    }

    private static OnboardingPromotionContext promotionContext() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(7L)
                        .tenantId(1L)
                        .userId(42L)
                        .targetOrganizationId(99L)
                        .build();
        return new OnboardingPromotionContext(invitation, 1L, 42L, 99L);
    }

    private static TenantOnboardingDocumentDefinition definition(String configKey) {
        return TenantOnboardingDocumentDefinition.builder()
                .tenantId(1L)
                .configKey(configKey)
                .name("Doc")
                .description("")
                .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                .mandatory(true)
                .active(true)
                .archived(false)
                .version(1)
                .build();
    }
}
