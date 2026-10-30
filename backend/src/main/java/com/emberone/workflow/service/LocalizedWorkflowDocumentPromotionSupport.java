/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.service.UserDocumentSystemWriter;
import com.emberone.userdocument.util.UserDocumentFileNames;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Shared i18n helpers for {@link
 * com.emberone.workflow.spi.WorkflowHandler#resolveDocumentPromotionMetadata}. Handlers with static
 * promotion labels use {@link #fromMessagePrefix}; handlers with payload-driven titles compose
 * {@link #fromInterpolatedTitle} or {@link #fromMessagePrefixWithTitleFallback}.
 */
@Service
@RequiredArgsConstructor
public class LocalizedWorkflowDocumentPromotionSupport {

    private final TenantLocaleService tenantLocaleService;
    private final LocalizationService localizationService;
    private final UserDocumentSystemWriter userDocumentSystemWriter;

    public Optional<WorkflowDocumentPromotionMetadata> fromMessagePrefix(
            Workflow workflow, String prefix) {
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(workflow.getTenantId());
        String title = localizationService.getMessageOrNull(prefix + ".title", locale);
        return buildMetadata(locale, prefix, title);
    }

    public Optional<WorkflowDocumentPromotionMetadata> fromInterpolatedTitle(
            Workflow workflow, String prefix, Map<String, Object> titleParams) {
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(workflow.getTenantId());
        String title =
                localizationService.getInterpolatedMessage(prefix + ".title", titleParams, locale);
        return buildMetadata(locale, prefix, title);
    }

    public Optional<WorkflowDocumentPromotionMetadata> fromMessagePrefixWithTitleFallback(
            Workflow workflow, String prefix) {
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(workflow.getTenantId());
        String title = localizationService.getMessageOrNull(prefix + ".titleFallback", locale);
        if (!StringUtils.hasText(title)) {
            title = localizationService.getMessageOrNull(prefix + ".title", locale);
        }
        return buildMetadata(locale, prefix, title);
    }

    /**
     * Applies localized promotion title, description, tag, and display filename to an existing user
     * document after a trusted system promotion links it to a record.
     */
    public void applyToDocument(
            UserDocument document,
            Long tenantId,
            String prefix,
            String nameParam,
            String titleLastResort) {
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(tenantId);
        String title = resolveDocumentTitle(locale, prefix, nameParam, titleLastResort);
        if (StringUtils.hasText(title)) {
            String normalizedTitle = title.trim();
            document.setTitle(normalizedTitle);
            document.setFileName(
                    UserDocumentFileNames.displayFileName(
                            normalizedTitle,
                            EntityAttachmentService.getFileExtension(document.getFileName()),
                            document.getFileName()));
        }

        String description = localizationService.getMessageOrNull(prefix + ".description", locale);
        document.setDescription(StringUtils.hasText(description) ? description.trim() : null);

        String tag = localizationService.getMessageOrNull(prefix + ".tag", locale);
        userDocumentSystemWriter.applyTagIfAbsent(document, tag);
    }

    private String resolveDocumentTitle(
            SupportedLocale locale, String prefix, String nameParam, String titleLastResort) {
        String template = localizationService.getMessageOrNull(prefix + ".title", locale);
        if (StringUtils.hasText(template)) {
            if (StringUtils.hasText(nameParam)) {
                return localizationService
                        .getInterpolatedMessage(
                                prefix + ".title", Map.of("name", nameParam.trim()), locale)
                        .trim();
            }
            return template.trim();
        }
        String fallback = localizationService.getMessageOrNull(prefix + ".titleFallback", locale);
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return StringUtils.hasText(titleLastResort) ? titleLastResort.trim() : null;
    }

    private Optional<WorkflowDocumentPromotionMetadata> buildMetadata(
            SupportedLocale locale, String prefix, String title) {
        if (!StringUtils.hasText(title)) {
            return Optional.empty();
        }

        String description = localizationService.getMessageOrNull(prefix + ".description", locale);
        String tag = localizationService.getMessageOrNull(prefix + ".tag", locale);
        List<String> tags = StringUtils.hasText(tag) ? List.of(tag.trim()) : List.of();

        return Optional.of(
                WorkflowDocumentPromotionMetadata.builder()
                        .title(title.trim())
                        .description(description != null ? description.trim() : null)
                        .tags(tags)
                        .build());
    }
}
