/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.rankrequest;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.rank.service.UserRankService;
import com.emberone.rank.tenant.TenantRank;
import com.emberone.rank.tenant.TenantRankRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class RankRequestWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<RankRequestWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX =
            "workflow.documentPromotion.rankRequest";
    private static final String APPROVED_REASON_KEY = "rank.request.approvedReason";

    private final WorkflowDocumentService workflowDocumentService;
    private final UserRankService userRankService;
    private final TenantRankRepository tenantRankRepository;
    private final LocalizationService localizationService;
    private final TenantLocaleService tenantLocaleService;

    public RankRequestWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            UserRankService userRankService,
            TenantRankRepository tenantRankRepository,
            LocalizationService localizationService,
            TenantLocaleService tenantLocaleService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.userRankService = userRankService;
        this.tenantRankRepository = tenantRankRepository;
        this.localizationService = localizationService;
        this.tenantLocaleService = tenantLocaleService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.RANK_REQUEST;
    }

    @Override
    public boolean allowsMultipleConcurrentOpen() {
        return false;
    }

    @Override
    public Class<RankRequestWorkflowPayload> payloadType() {
        return RankRequestWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - rank request is not possible";
    }

    @Override
    public Optional<com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata>
            resolveDocumentPromotionMetadata(Workflow workflow) {
        return documentPromotionMetadataSupport.fromMessagePrefixWithTitleFallback(
                workflow, DOCUMENT_PROMOTION_PREFIX);
    }

    @Override
    public Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        return requireMainOrganization(openedByUserId);
    }

    @Override
    public Long resolveContextOrganization(WorkflowOpenContext<RankRequestWorkflowPayload> ctx) {
        requirePayloadPresent(ctx.getPayload());
        return requireMainOrganization(ctx.getOpenedByUserId());
    }

    @Override
    public void validateOpen(WorkflowOpenContext<RankRequestWorkflowPayload> ctx) {
        RankRequestWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        if (userRankService.hasOpenRank(ctx.getOpenedByUserId())) {
            throw new WorkflowValidationException("User already has a rank");
        }
        validatePayload(payload);
        validateRank(payload.getTenantRankId());
        Long mainOrgId = requireMainOrganization(ctx.getOpenedByUserId());
        validateOptionalAttachments(ctx.getOpenedByUserId(), mainOrgId, payload.getAttachmentIds());
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<RankRequestWorkflowPayload> ctx, Workflow existing) {
        RankRequestWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        requireOpeningUser(ctx);
        if (userRankService.hasOpenRank(ctx.getOpenedByUserId())) {
            throw new WorkflowValidationException("User already has a rank");
        }
        validatePayload(payload);
        validateRank(payload.getTenantRankId());
        Long mainOrgId = requireMainOrganization(ctx.getOpenedByUserId());
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                mainOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                RankRequestWorkflowPayload.MAX_ATTACHMENTS,
                false);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachRankRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachRankRequestAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        if (userRankService.hasOpenRank(workflow.getOpenedByUserId())) {
            throw new WorkflowValidationException("User already has a rank");
        }
        RankRequestWorkflowPayload payload =
                readPayload(workflow, RankRequestWorkflowPayload.class);
        LocalDate startDate =
                workflow.getDecidedAt() != null
                        ? workflow.getDecidedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                        : LocalDate.now();
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(workflow.getTenantId());
        String reason = localizationService.getMessage(APPROVED_REASON_KEY, locale);
        userRankService.assignFromWorkflow(
                workflow.getOpenedByUserId(), payload.getTenantRankId(), startDate, reason);
        log.info(
                "Assigned first rank for user {} via approved workflow {}",
                workflow.getOpenedByUserId(),
                workflow.getId());
    }

    private void validatePayload(RankRequestWorkflowPayload payload) {
        if (payload.getTenantRankId() == null) {
            throw new WorkflowValidationException("tenantRankId is required");
        }
    }

    private void validateRank(long tenantRankId) {
        TenantRank rank =
                tenantRankRepository
                        .findById(tenantRankId)
                        .orElseThrow(() -> new WorkflowValidationException("Unknown rank"));
        if (!LifecycleFields.isCatalogSelectable(rank)) {
            throw new WorkflowValidationException("Rank is not active");
        }
    }

    private Long requireMainOrganization(long userId) {
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, Instant.now())
                        .orElseThrow(
                                () ->
                                        new WorkflowValidationException(
                                                "Main organization membership is required"));
        return membership.getOrganization().getId();
    }

    private void validateOptionalAttachments(
            long userId, long organizationId, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        validateWorkflowDraftAttachments(
                userId,
                organizationId,
                attachmentIds,
                RankRequestWorkflowPayload.MAX_ATTACHMENTS,
                false);
    }
}
