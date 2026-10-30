/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.namechange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.NameChangeWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class NameChangeWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock UserService userService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private NameChangeWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new NameChangeWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        userService,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesStaticNameChangeMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.NAME_CHANGE);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.STATIC);
        assertThat(handler.payloadType()).isEqualTo(NameChangeWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isFalse();
        assertThat(handler.listedInBeginnableCatalog()).isFalse();
        assertThat(handler.requiresOpenerOrganizationMembership()).isTrue();
        assertThat(
                        handler.resolveDocumentPromotionDefaults(Workflow.builder().build())
                                .isIncludeWorkflowMedia())
                .isTrue();
    }

    @Test
    void validateOpenNormalizesNameSnapshotsPreviousValuesAndChecksAttachmentOwnership() {
        User user = User.builder().id(1L).tenantId(7L).firstName("Old").lastName("Name").build();
        Organization organization = Organization.builder().id(10L).build();
        NameChangeWorkflowPayload payload =
                NameChangeWorkflowPayload.builder()
                        .firstName(" New ")
                        .lastName(" Person ")
                        .attachmentIds(List.of(20L))
                        .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));
        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        20L, EntityAttachmentType.WORKFLOW_DRAFT, 1L, 10L))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(1L).build()));

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getFirstName()).isEqualTo(" New ");
        assertThat(payload.getLastName()).isEqualTo(" Person ");
        assertThat(payload.getPreviousFirstName()).isEqualTo("Old");
        assertThat(payload.getPreviousLastName()).isEqualTo("Name");
    }

    @Test
    void validateOpenRejectsMissingPayloadUserWrongTenantUnchangedNameAndInvalidAttachments() {
        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, null)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Payload is required");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("A")
                                                        .lastName("B")
                                                        .attachmentIds(List.of(1L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("User not found");

        User user = User.builder().id(1L).tenantId(7L).firstName("Old").lastName("Name").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                8L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("New")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of(1L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Tenant");
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName(" ")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of(1L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("Old")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of(1L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("differ");

        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("New")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of())
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("attachment");

        List<Long> tooMany =
                IntStream.rangeClosed(1, NameChangeWorkflowPayload.MAX_ATTACHMENTS + 1)
                        .mapToObj(Long::valueOf)
                        .toList();
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("New")
                                                        .lastName("Name")
                                                        .attachmentIds(tooMany)
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("At most");
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("New")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of(2L, 2L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Duplicate");

        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        99L, EntityAttachmentType.WORKFLOW_DRAFT, 1L, 10L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("New")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of(99L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Attachment not found");

        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        20L, EntityAttachmentType.WORKFLOW_DRAFT, 1L, 10L))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(2L).build()));
        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("New")
                                                        .lastName("Name")
                                                        .attachmentIds(List.of(20L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not uploaded by you");
    }

    @Test
    void resolveContextOrganizationRequiresMainMembership() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                handler.resolveContextOrganization(
                                        new WorkflowOpenContext<>(
                                                1L,
                                                7L,
                                                NameChangeWorkflowPayload.builder()
                                                        .firstName("A")
                                                        .lastName("B")
                                                        .attachmentIds(List.of(1L))
                                                        .build())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("No organization membership");
    }

    @Test
    void openedWorkflowAttachesDocumentsAndApprovedWorkflowAppliesParsedName() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload(
                                "{\"firstName\":\" New \",\"lastName\":\" Person \",\"attachmentIds\":[20]}")
                        .build();

        handler.onOpened(workflow);
        handler.onApproved(workflow);

        verify(workflowDocumentService).attachNameChangeAttachmentsToWorkflow(workflow);
        verify(userService).applyApprovedNameChange(1L, 7L, "New", "Person");
    }

    @Test
    void onApprovedRejectsMissingOrInvalidPayload() {
        Workflow missing =
                Workflow.builder().id(5L).tenantId(7L).openedByUserId(1L).payload(" ").build();
        Workflow invalid =
                Workflow.builder().id(6L).tenantId(7L).openedByUserId(1L).payload("{").build();

        assertThatThrownBy(() -> handler.onApproved(missing))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("payload missing");
        assertThatThrownBy(() -> handler.onApproved(invalid))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Invalid workflow payload");
    }
}
