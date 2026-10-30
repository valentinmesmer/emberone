/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentOutcome;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentProof;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.UserEntryDocumentFulfillmentService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.userdocument.dto.UserDocumentLinkInput;
import com.emberone.userdocument.service.UserDocumentPromotionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingAttachmentPromotionService {

    private final UserDocumentPromotionService userDocumentPromotionService;
    private final TenantOnboardingDocumentDefinitionRepository documentDefinitionRepository;
    private final OrganizationOnboardingDocumentDefinitionRepository
            organizationDocumentDefinitionRepository;
    private final OnboardingDocumentTemplateService tenantTemplateService;
    private final OrganizationOnboardingDocumentTemplateService organizationTemplateService;
    private final UserEntryDocumentFulfillmentService entryDocumentFulfillmentService;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    @Transactional
    public Long copyAttachmentToUserDocument(
            long tenantId,
            long userId,
            long invitationId,
            String storageKey,
            String fileName,
            String contentType,
            Long sizeBytes,
            String title,
            List<String> tags) {
        Long documentId =
                userDocumentPromotionService.copyFromStorage(
                        tenantId,
                        userId,
                        attachmentsBucket,
                        storageKey,
                        fileName,
                        contentType,
                        sizeBytes,
                        title,
                        null,
                        tags,
                        List.of(
                                UserDocumentLinkInput.builder()
                                        .linkType(AppLinkType.ONBOARDING_INVITATION)
                                        .linkId(invitationId)
                                        .build()));
        if (documentId != null) {
            log.debug(
                    "Copied onboarding attachment for invitation {} to user document {}",
                    invitationId,
                    documentId);
        }
        return documentId;
    }

    @Transactional
    public Long copyDraftAttachment(
            long tenantId, long userId, long invitationId, Map<String, Object> record) {
        return copyAttachmentToUserDocument(
                tenantId,
                userId,
                invitationId,
                OnboardingStepPayloads.readNullableString(
                        record, OnboardingStepPayloads.STORAGE_KEY),
                OnboardingStepPayloads.readNullableString(record, OnboardingStepPayloads.FILE_NAME),
                OnboardingStepPayloads.readNullableString(
                        record, OnboardingStepPayloads.CONTENT_TYPE),
                OnboardingStepPayloads.readLong(record, OnboardingStepPayloads.SIZE_BYTES),
                null,
                List.of());
    }

    @Transactional
    public void recordDeclinedTenantEntryDocument(long tenantId, long userId, String configKey) {
        documentDefinitionRepository
                .findByConfigKey(configKey)
                .ifPresent(
                        definition ->
                                tenantTemplateService
                                        .findCurrentTemplateMetadata(definition)
                                        .ifPresent(
                                                template ->
                                                        entryDocumentFulfillmentService
                                                                .recordDecline(
                                                                        tenantId,
                                                                        null,
                                                                        userId,
                                                                        configKey,
                                                                        EntryDocumentFulfillmentProof
                                                                                .selfDecline(
                                                                                        template
                                                                                                .version(),
                                                                                        template
                                                                                                .sha256Hex(),
                                                                                        userId))));
    }

    @Transactional
    public void recordDeclinedOrganizationEntryDocument(
            long tenantId, long organizationId, long userId, String configKey) {
        organizationDocumentDefinitionRepository
                .findByOrganizationIdAndConfigKey(organizationId, configKey)
                .ifPresent(
                        definition ->
                                organizationTemplateService
                                        .findCurrentTemplateMetadata(definition)
                                        .ifPresent(
                                                template ->
                                                        entryDocumentFulfillmentService
                                                                .recordDecline(
                                                                        tenantId,
                                                                        organizationId,
                                                                        userId,
                                                                        configKey,
                                                                        EntryDocumentFulfillmentProof
                                                                                .selfDecline(
                                                                                        template
                                                                                                .version(),
                                                                                        template
                                                                                                .sha256Hex(),
                                                                                        userId))));
    }

    @Transactional
    public void upsertConfirmedEntryDocument(
            OnboardingPromotionContext context, String configKey, Map<String, Object> document) {
        documentDefinitionRepository
                .findByConfigKey(configKey)
                .ifPresent(
                        definition ->
                                tenantTemplateService
                                        .findCurrentTemplateMetadata(definition)
                                        .ifPresent(
                                                template ->
                                                        entryDocumentFulfillmentService
                                                                .upsertTenant(
                                                                        context.tenantId(),
                                                                        context.userId(),
                                                                        configKey,
                                                                        OnboardingDocumentFulfillmentMode
                                                                                .CONFIRMED,
                                                                        null,
                                                                        buildProof(
                                                                                template,
                                                                                document,
                                                                                context,
                                                                                OnboardingDocumentFulfillmentMode
                                                                                        .CONFIRMED))));
    }

    @Transactional
    public void upsertSignedEntryDocument(
            OnboardingPromotionContext context,
            String configKey,
            Map<String, Object> document,
            String storageKey,
            String fileName,
            String contentType,
            Long sizeBytes,
            String title) {
        documentDefinitionRepository
                .findByConfigKey(configKey)
                .ifPresent(
                        definition -> {
                            OnboardingDocumentTemplateService.TemplateMetadata template =
                                    tenantTemplateService
                                            .findCurrentTemplateMetadata(definition)
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalStateException(
                                                                    "Missing template for: "
                                                                            + configKey));
                            Long documentId =
                                    copyAttachmentToUserDocument(
                                            context.tenantId(),
                                            context.userId(),
                                            context.invitationId(),
                                            storageKey,
                                            fileName,
                                            contentType,
                                            sizeBytes,
                                            title,
                                            List.of(title));
                            entryDocumentFulfillmentService.upsertTenant(
                                    context.tenantId(),
                                    context.userId(),
                                    configKey,
                                    OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD,
                                    documentId,
                                    buildProof(
                                            template,
                                            document,
                                            context,
                                            OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD));
                        });
    }

    @Transactional
    public void upsertConfirmedOrganizationEntryDocument(
            OnboardingPromotionContext context, String configKey, Map<String, Object> document) {
        organizationDocumentDefinitionRepository
                .findByOrganizationIdAndConfigKey(context.organizationId(), configKey)
                .ifPresent(
                        definition ->
                                organizationTemplateService
                                        .findCurrentTemplateMetadata(definition)
                                        .ifPresent(
                                                template ->
                                                        entryDocumentFulfillmentService
                                                                .upsertOrganization(
                                                                        context.tenantId(),
                                                                        context.organizationId(),
                                                                        context.userId(),
                                                                        configKey,
                                                                        OnboardingDocumentFulfillmentMode
                                                                                .CONFIRMED,
                                                                        null,
                                                                        buildProof(
                                                                                template,
                                                                                document,
                                                                                context,
                                                                                OnboardingDocumentFulfillmentMode
                                                                                        .CONFIRMED))));
    }

    @Transactional
    public void upsertSignedOrganizationEntryDocument(
            OnboardingPromotionContext context,
            String configKey,
            Map<String, Object> document,
            String storageKey,
            String fileName,
            String contentType,
            Long sizeBytes,
            String title) {
        organizationDocumentDefinitionRepository
                .findByOrganizationIdAndConfigKey(context.organizationId(), configKey)
                .ifPresent(
                        definition -> {
                            OnboardingDocumentTemplateService.TemplateMetadata template =
                                    organizationTemplateService
                                            .findCurrentTemplateMetadata(definition)
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalStateException(
                                                                    "Missing template for: "
                                                                            + configKey));
                            Long documentId =
                                    copyAttachmentToUserDocument(
                                            context.tenantId(),
                                            context.userId(),
                                            context.invitationId(),
                                            storageKey,
                                            fileName,
                                            contentType,
                                            sizeBytes,
                                            title,
                                            List.of(title));
                            entryDocumentFulfillmentService.upsertOrganization(
                                    context.tenantId(),
                                    context.organizationId(),
                                    context.userId(),
                                    configKey,
                                    OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD,
                                    documentId,
                                    buildProof(
                                            template,
                                            document,
                                            context,
                                            OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD));
                        });
    }

    /**
     * Admin-recorded onboarding capture (no UI yet for per-document admin proof). Reserved for
     * future admin entry-document recording flows.
     */
    @Transactional
    public void recordAdminCapturedEntryDocument(
            long tenantId,
            Long organizationId,
            long userId,
            String configKey,
            OnboardingDocumentFulfillmentMode mode,
            Long userDocumentId,
            EntryDocumentFulfillmentProof proof) {
        if (proof.channel() != EntryDocumentFulfillmentChannel.ADMIN_RECORDED) {
            throw new IllegalArgumentException("Admin capture requires ADMIN_RECORDED channel");
        }
        if (organizationId == null) {
            entryDocumentFulfillmentService.upsertTenant(
                    tenantId, userId, configKey, mode, userDocumentId, proof);
        } else {
            entryDocumentFulfillmentService.upsertOrganization(
                    tenantId, organizationId, userId, configKey, mode, userDocumentId, proof);
        }
    }

    private EntryDocumentFulfillmentProof buildProof(
            OnboardingDocumentTemplateService.TemplateMetadata template,
            Map<String, Object> document,
            OnboardingPromotionContext context,
            OnboardingDocumentFulfillmentMode mode) {
        Integer draftVersion =
                OnboardingStepPayloads.readInteger(
                        document, OnboardingStepPayloads.DOCUMENT_VERSION);
        if (draftVersion == null || draftVersion != template.version()) {
            throw new ConflictException("Entry document draft version does not match template");
        }
        Instant confirmedAt =
                OnboardingStepPayloads.readInstant(document, OnboardingStepPayloads.CONFIRMED_AT);
        if (confirmedAt == null) {
            confirmedAt = Instant.now();
        }
        String confirmationText =
                OnboardingStepPayloads.readNullableString(
                        document, OnboardingStepPayloads.CONFIRMATION_TEXT);
        if (confirmationText == null) {
            confirmationText = "";
        }
        long actorUserId = context.userId();
        Long workflowReviewerUserId = null;
        Instant workflowReviewedAt = null;
        if (context.entryDocumentChannel() == EntryDocumentFulfillmentChannel.ADMIN_RECORDED
                && context.workflowReviewerUserId() != null) {
            actorUserId = context.workflowReviewerUserId();
        } else if (mode == OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD
                && context.workflowReviewerUserId() != null) {
            workflowReviewerUserId = context.workflowReviewerUserId();
            workflowReviewedAt = Instant.now();
        }
        return EntryDocumentFulfillmentProof.builder()
                .templateVersion(template.version())
                .templateSha256Hex(template.sha256Hex())
                .confirmationText(confirmationText)
                .outcome(EntryDocumentFulfillmentOutcome.ACCEPTED)
                .channel(context.entryDocumentChannel())
                .actorUserId(actorUserId)
                .adminRecordReason(context.adminRecordReason())
                .workflowReviewerUserId(workflowReviewerUserId)
                .workflowReviewedAt(workflowReviewedAt)
                .confirmedAt(confirmedAt)
                .build();
    }
}
