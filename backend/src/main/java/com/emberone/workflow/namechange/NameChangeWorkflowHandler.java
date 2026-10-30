/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.namechange;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.NameChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.AbstractMembershipContextWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class NameChangeWorkflowHandler
        extends AbstractMembershipContextWorkflowHandler<NameChangeWorkflowPayload> {

    private static final String DOCUMENT_PROMOTION_PREFIX = "workflow.documentPromotion.nameChange";

    private final WorkflowDocumentService workflowDocumentService;
    private final UserService userService;

    public NameChangeWorkflowHandler(
            UserRepository userRepository,
            UserOrganizationMembershipRepository membershipRepository,
            EntityAttachmentRepository entityAttachmentRepository,
            LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport,
            WorkflowDocumentService workflowDocumentService,
            UserService userService,
            JsonMapper jsonMapper) {
        super(
                userRepository,
                membershipRepository,
                entityAttachmentRepository,
                documentPromotionMetadataSupport,
                jsonMapper);
        this.workflowDocumentService = workflowDocumentService;
        this.userService = userService;
    }

    @Override
    public WorkflowType workflowType() {
        return WorkflowType.NAME_CHANGE;
    }

    @Override
    public Class<NameChangeWorkflowPayload> payloadType() {
        return NameChangeWorkflowPayload.class;
    }

    @Override
    protected String missingMembershipMessage() {
        return "No organization membership - name change is not possible";
    }

    @Override
    public Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        return resolveLocalizedDocumentPromotionMetadata(workflow, DOCUMENT_PROMOTION_PREFIX);
    }

    @Override
    public void validateOpen(WorkflowOpenContext<NameChangeWorkflowPayload> ctx) {
        NameChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        String firstName = payload.getFirstName() != null ? payload.getFirstName().trim() : "";
        String lastName = payload.getLastName() != null ? payload.getLastName().trim() : "";
        if (firstName.isEmpty() || lastName.isEmpty()) {
            throw new WorkflowValidationException("First name and last name must not be empty");
        }

        User user = requireOpeningUser(ctx);
        if (firstName.equalsIgnoreCase(user.getFirstName())
                && lastName.equalsIgnoreCase(user.getLastName())) {
            throw new WorkflowValidationException("The new name must differ from the current name");
        }

        payload.setPreviousFirstName(user.getFirstName());
        payload.setPreviousLastName(user.getLastName());

        Long contextOrgId = resolveContextOrganization(ctx);
        validateWorkflowDraftAttachments(
                ctx.getOpenedByUserId(),
                contextOrgId,
                payload.getAttachmentIds(),
                NameChangeWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void validateUpdate(
            WorkflowOpenContext<NameChangeWorkflowPayload> ctx, Workflow existing) {
        NameChangeWorkflowPayload payload = ctx.getPayload();
        requirePayloadPresent(payload);
        String firstName = payload.getFirstName() != null ? payload.getFirstName().trim() : "";
        String lastName = payload.getLastName() != null ? payload.getLastName().trim() : "";
        if (firstName.isEmpty() || lastName.isEmpty()) {
            throw new WorkflowValidationException("First name and last name must not be empty");
        }

        User user = requireOpeningUser(ctx);
        if (firstName.equalsIgnoreCase(user.getFirstName())
                && lastName.equalsIgnoreCase(user.getLastName())) {
            throw new WorkflowValidationException("The new name must differ from the current name");
        }

        payload.setPreviousFirstName(user.getFirstName());
        payload.setPreviousLastName(user.getLastName());

        Long contextOrgId = resolveContextOrganization(ctx);
        validateWorkflowAttachmentsForUpdate(
                ctx.getOpenedByUserId(),
                contextOrgId,
                existing.getId(),
                payload.getAttachmentIds(),
                NameChangeWorkflowPayload.MAX_ATTACHMENTS,
                true);
    }

    @Override
    public void onOpened(Workflow workflow) {
        workflowDocumentService.attachNameChangeAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onInputUpdated(Workflow workflow) {
        workflowDocumentService.attachNameChangeAttachmentsToWorkflow(workflow);
    }

    @Override
    public void onApproved(Workflow workflow) {
        NameChangeWorkflowPayload payload = readPayload(workflow, NameChangeWorkflowPayload.class);
        userService.applyApprovedNameChange(
                workflow.getOpenedByUserId(),
                workflow.getTenantId(),
                payload.getFirstName().trim(),
                payload.getLastName().trim());
        log.info(
                "Applied approved name change for user {} via workflow {}",
                workflow.getOpenedByUserId(),
                workflow.getId());
    }
}
