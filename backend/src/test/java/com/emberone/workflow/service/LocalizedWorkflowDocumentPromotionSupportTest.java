/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.userdocument.service.UserDocumentSystemWriter;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalizedWorkflowDocumentPromotionSupportTest {

    @Mock TenantLocaleService tenantLocaleService;
    @Mock LocalizationService localizationService;
    @Mock UserDocumentSystemWriter userDocumentSystemWriter;

    private LocalizedWorkflowDocumentPromotionSupport support;

    private static Workflow workflow(Long tenantId) {
        return Workflow.builder()
                .id(1L)
                .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                .tenantId(tenantId)
                .build();
    }

    @BeforeEach
    void setUp() {
        support =
                new LocalizedWorkflowDocumentPromotionSupport(
                        tenantLocaleService, localizationService, userDocumentSystemWriter);
    }

    @Test
    void fromMessagePrefixReturnsLocalizedMetadata() {
        Workflow workflow = workflow(7L);
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.nameChange.title", SupportedLocale.DE))
                .thenReturn(" Namensänderung ");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.nameChange.description", SupportedLocale.DE))
                .thenReturn(" Beschreibung ");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.nameChange.tag", SupportedLocale.DE))
                .thenReturn(" Personaldokument ");

        Optional<WorkflowDocumentPromotionMetadata> metadata =
                support.fromMessagePrefix(workflow, "workflow.documentPromotion.nameChange");

        assertThat(metadata).isPresent();
        assertThat(metadata.get().getTitle()).isEqualTo("Namensänderung");
        assertThat(metadata.get().getDescription()).isEqualTo("Beschreibung");
        assertThat(metadata.get().getTags()).containsExactly("Personaldokument");
    }

    @Test
    void fromInterpolatedTitleUsesDefinitionName() {
        Workflow workflow = workflow(7L);
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getInterpolatedMessage(
                        "workflow.documentPromotion.certificationRecord.title",
                        Map.of("name", "Atemschutzgeräteträger"),
                        SupportedLocale.DE))
                .thenReturn("Nachweis Atemschutzgeräteträger");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.description",
                        SupportedLocale.DE))
                .thenReturn("Nachweis aus einer Einreichung");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.tag", SupportedLocale.DE))
                .thenReturn("Nachweis");

        Optional<WorkflowDocumentPromotionMetadata> metadata =
                support.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.certificationRecord",
                        Map.of("name", "Atemschutzgeräteträger"));

        assertThat(metadata).isPresent();
        assertThat(metadata.get().getTitle()).isEqualTo("Nachweis Atemschutzgeräteträger");
        assertThat(metadata.get().getTags()).containsExactly("Nachweis");
    }

    @Test
    void fromMessagePrefixWithTitleFallbackUsesFallbackWhenNeeded() {
        Workflow workflow = workflow(7L);
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.titleFallback",
                        SupportedLocale.DE))
                .thenReturn("Eingereichter Nachweis");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.description",
                        SupportedLocale.DE))
                .thenReturn(null);
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.tag", SupportedLocale.DE))
                .thenReturn("Nachweis");

        Optional<WorkflowDocumentPromotionMetadata> metadata =
                support.fromMessagePrefixWithTitleFallback(
                        workflow, "workflow.documentPromotion.certificationRecord");

        assertThat(metadata).isPresent();
        assertThat(metadata.get().getTitle()).isEqualTo("Eingereichter Nachweis");
    }

    @Test
    void fromMessagePrefixReturnsEmptyWhenTitleMissing() {
        Workflow workflow = workflow(7L);
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.nameChange.title", SupportedLocale.DE))
                .thenReturn("   ");

        Optional<WorkflowDocumentPromotionMetadata> metadata =
                support.fromMessagePrefix(workflow, "workflow.documentPromotion.nameChange");

        assertThat(metadata).isEmpty();
    }
}
