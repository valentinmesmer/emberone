/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowComment;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowCommentRepository;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowDocumentServiceTest {

    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowCommentRepository commentRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock ObjectStorageService storageService;
    @Mock ObjectStorageDeletionOutboxService deletionOutboxService;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock TokenService tokenService;
    private EntityAttachmentService entityAttachmentService;
    private WorkflowDocumentService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        entityAttachmentService =
                new EntityAttachmentService(
                        attachmentRepository,
                        storageService,
                        new com.emberone.platform.storage.service
                                .ObjectStorageTransactionCoordinator(
                                storageService, deletionOutboxService));
        service =
                new WorkflowDocumentService(
                        workflowRepository,
                        commentRepository,
                        attachmentRepository,
                        entityAttachmentService,
                        membershipRepository,
                        workflowDefinitionRepository,
                        tokenService,
                        JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "workflowsBucket", "workflows");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void uploadDraftRequiresMembershipAndCreatesTenantScopedAttachment() {
        when(tokenService.getTenantId()).thenReturn(7L);
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        when(attachmentRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            EntityAttachment attachment = invocation.getArgument(0);
                            attachment.setId(5L);
                            return attachment;
                        });
        MockMultipartFile file =
                new MockMultipartFile("file", "proof.pdf", "application/pdf", "data".getBytes());

        var dto = service.uploadWorkflowAttachmentDraft(1L, WorkflowType.NAME_CHANGE.getId(), file);

        assertThat(dto.getId()).isEqualTo(5L);
        var captor = org.mockito.ArgumentCaptor.forClass(EntityAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityType())
                .isEqualTo(EntityAttachmentType.WORKFLOW_DRAFT);
        assertThat(captor.getValue().getEntityId()).isEqualTo(1L);
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(10L);
        assertThat(captor.getValue().getStorageKey())
                .startsWith("tenants/7/workflows/drafts/users/1/name-change/")
                .endsWith(".pdf");
    }

    @Test
    void uploadDraftRejectsUnsupportedMediaAndMissingMembership() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.uploadWorkflowAttachmentDraft(
                                        1L,
                                        WorkflowType.NAME_CHANGE.getId(),
                                        new MockMultipartFile(
                                                "file",
                                                "proof.pdf",
                                                "application/pdf",
                                                "data".getBytes())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("membership");

        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        assertThatThrownBy(
                        () ->
                                service.uploadWorkflowAttachmentDraft(
                                        1L,
                                        WorkflowType.NAME_CHANGE.getId(),
                                        new MockMultipartFile(
                                                "file",
                                                "proof.exe",
                                                "application/octet-stream",
                                                "data".getBytes())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void uploadJoinRequestDraftWithoutMembershipUsesEnabledTargetOrganization() {
        when(tokenService.getTenantId()).thenReturn(7L);
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(workflowDefinitionRepository.findByWorkflowTypeId(
                        WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .enabled(true)
                                        .organizationId(20L)
                                        .build()));
        when(attachmentRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            EntityAttachment attachment = invocation.getArgument(0);
                            attachment.setId(6L);
                            return attachment;
                        });

        var dto =
                service.uploadWorkflowAttachmentDraft(
                        1L,
                        WorkflowType.ORGANIZATION_JOIN_REQUEST.getId(),
                        new MockMultipartFile(
                                "file", "proof.pdf", "application/pdf", "data".getBytes()));

        assertThat(dto.getId()).isEqualTo(6L);
        var captor = org.mockito.ArgumentCaptor.forClass(EntityAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(20L);
        assertThat(captor.getValue().getStorageKey())
                .startsWith("tenants/7/workflows/drafts/users/1/organization-join-request/")
                .endsWith(".pdf");
    }

    @Test
    void nameChangeDraftIsMovedAndRelinkedToWorkflow() {
        Workflow workflow =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .payload(
                                "{\"firstName\":\"New\",\"lastName\":\"Name\",\"attachmentIds\":[5]}")
                        .build();
        EntityAttachment draft = draft(5L);
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(attachmentRepository.save(draft)).thenReturn(draft);

        service.attachNameChangeAttachmentsToWorkflow(workflow);

        assertThat(draft.getEntityType()).isEqualTo(EntityAttachmentType.WORKFLOW);
        assertThat(draft.getEntityId()).isEqualTo(20L);
        assertThat(draft.getStorageKey()).startsWith("tenants/7/workflows/20/").endsWith(".pdf");
        verify(storageService)
                .copyFile(
                        eq("workflows"),
                        eq("tenants/7/workflows/drafts/draft-key"),
                        eq("workflows"),
                        org.mockito.ArgumentMatchers.argThat(
                                key ->
                                        key.toString().startsWith("tenants/7/workflows/20/")
                                                && key.toString().endsWith(".pdf")));
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "workflows", "tenants/7/workflows/drafts/draft-key");
        verify(attachmentRepository).save(draft);
    }

    @Test
    void draftOwnedByAnotherUserCannotBeAttached() {
        Workflow workflow =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .payload(
                                "{\"firstName\":\"New\",\"lastName\":\"Name\",\"attachmentIds\":[5]}")
                        .build();
        EntityAttachment draft = draft(5L);
        draft.setUploadedBy(2L);
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.attachNameChangeAttachmentsToWorkflow(workflow))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("uploaded");
    }

    @Test
    void listFallsBackToPayloadDraftOnlyWhileWorkflowIsOpenOrRejected() {
        Workflow workflow =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .status(WorkflowStatus.OPEN)
                        .payload(
                                "{\"firstName\":\"New\",\"lastName\":\"Name\",\"attachmentIds\":[5]}")
                        .build();
        EntityAttachment draft = draft(5L);
        when(workflowRepository.findWithDetailsById(20L)).thenReturn(Optional.of(workflow));
        when(attachmentRepository.findByEntityTypeAndEntityId(EntityAttachmentType.WORKFLOW, 20L))
                .thenReturn(List.of());
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(draft));

        assertThat(service.listWorkflowAttachments(20L))
                .extracting(EntityAttachmentDto::getId)
                .containsExactly(5L);

        workflow.setStatus(WorkflowStatus.APPROVED);
        assertThat(service.listWorkflowAttachments(20L)).isEmpty();
    }

    @Test
    void commentDraftMustBelongToWorkflowAndUserBeforeMove() {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();
        WorkflowComment comment = WorkflowComment.builder().id(30L).workflow(workflow).build();
        EntityAttachment draft =
                EntityAttachment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT_DRAFT)
                        .entityId(20L)
                        .fileName("comment.png")
                        .storageKey("tenants/7/workflows/comment-draft")
                        .uploadedBy(1L)
                        .build();
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(attachmentRepository.save(draft)).thenReturn(draft);

        service.attachCommentDraftToComment(workflow, comment, 5L, 1L);

        assertThat(draft.getEntityType()).isEqualTo(EntityAttachmentType.WORKFLOW_COMMENT);
        assertThat(draft.getEntityId()).isEqualTo(30L);
        verify(storageService)
                .copyFile(
                        eq("workflows"),
                        eq("tenants/7/workflows/comment-draft"),
                        eq("workflows"),
                        org.mockito.ArgumentMatchers.argThat(
                                key -> key.toString().contains("/workflows/20/")));
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "workflows", "tenants/7/workflows/comment-draft");
    }

    @Test
    void downloadsLinkedWorkflowAttachmentWithMetadata() throws Exception {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW)
                        .entityId(20L)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .storageKey("workflow-key")
                        .build();
        when(workflowRepository.findWithDetailsById(20L)).thenReturn(Optional.of(workflow));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));
        when(storageService.downloadFile("workflows", "workflow-key"))
                .thenReturn(new ByteArrayInputStream("data".getBytes()));

        var bundle = service.downloadWorkflowAttachment(20L, 5L);

        assertThat(bundle.fileName()).isEqualTo("proof.pdf");
        assertThat(bundle.contentType()).isEqualTo("application/pdf");
        assertThat(bundle.resource().getInputStream().readAllBytes()).isEqualTo("data".getBytes());
    }

    @Test
    void downloadRejectsAttachmentFromAnotherWorkflow() {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).workflowTypeId(99L).build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW)
                        .entityId(99L)
                        .build();
        when(workflowRepository.findWithDetailsById(20L)).thenReturn(Optional.of(workflow));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.downloadWorkflowAttachment(20L, 5L))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void uploadCommentAttachmentDraftCreatesWorkflowScopedDraft() {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        when(attachmentRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            EntityAttachment attachment = invocation.getArgument(0);
                            attachment.setId(8L);
                            return attachment;
                        });
        MockMultipartFile file =
                new MockMultipartFile("file", "note.png", "image/png", "data".getBytes());

        var dto = service.uploadWorkflowCommentAttachmentDraft(workflow, 1L, file);

        assertThat(dto.getId()).isEqualTo(8L);
        var captor = org.mockito.ArgumentCaptor.forClass(EntityAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityType())
                .isEqualTo(EntityAttachmentType.WORKFLOW_COMMENT_DRAFT);
        assertThat(captor.getValue().getEntityId()).isEqualTo(20L);
        assertThat(captor.getValue().getStorageKey())
                .startsWith("tenants/7/workflows/20/comment-drafts/users/1/")
                .endsWith(".png");
        verify(storageService).uploadFile(file, "workflows", captor.getValue().getStorageKey());
    }

    @Test
    void uploadCommentAttachmentDraftRejectsEmptyAndUnsupportedFiles() {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();

        assertThatThrownBy(
                        () ->
                                service.uploadWorkflowCommentAttachmentDraft(
                                        workflow,
                                        1L,
                                        new MockMultipartFile(
                                                "file", "empty.png", "image/png", new byte[0])))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(
                        () ->
                                service.uploadWorkflowCommentAttachmentDraft(
                                        workflow,
                                        1L,
                                        new MockMultipartFile(
                                                "file",
                                                "bad.exe",
                                                "application/octet-stream",
                                                "data".getBytes())))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void addressChangeDraftIsMovedAndRelinkedToWorkflow() {
        Workflow workflow =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.ADDRESS_CHANGE.getId())
                        .payload(
                                "{\"street\":\"New\",\"city\":\"City\",\"postalCode\":\"1\",\"country\":\"DE\",\"attachmentIds\":[5]}")
                        .build();
        EntityAttachment draft = draft(5L);
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(attachmentRepository.save(draft)).thenReturn(draft);

        service.attachAddressChangeAttachmentsToWorkflow(workflow);

        assertThat(draft.getEntityType()).isEqualTo(EntityAttachmentType.WORKFLOW);
        assertThat(draft.getEntityId()).isEqualTo(20L);
        verify(storageService)
                .copyFile(
                        eq("workflows"),
                        eq("tenants/7/workflows/drafts/draft-key"),
                        eq("workflows"),
                        org.mockito.ArgumentMatchers.argThat(
                                key -> key.toString().startsWith("tenants/7/workflows/20/")));
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "workflows", "tenants/7/workflows/drafts/draft-key");
        verify(attachmentRepository).save(draft);
    }

    @Test
    void addressChangeAttachNoOpsForWrongTypeOrMissingAttachments() {
        Workflow nameChange =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .payload("{\"firstName\":\"A\",\"lastName\":\"B\",\"attachmentIds\":[5]}")
                        .build();
        service.attachAddressChangeAttachmentsToWorkflow(nameChange);
        verify(attachmentRepository, org.mockito.Mockito.never()).save(any());

        Workflow withoutAttachments =
                Workflow.builder()
                        .id(21L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.ADDRESS_CHANGE.getId())
                        .payload(
                                "{\"street\":\"New\",\"city\":\"City\",\"postalCode\":\"1\",\"country\":\"DE\"}")
                        .build();
        service.attachAddressChangeAttachmentsToWorkflow(withoutAttachments);
        verify(attachmentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void certificationAttachAllowsMissingAttachments() {
        Workflow withoutAttachments =
                Workflow.builder()
                        .id(21L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.CERTIFICATION_RECORD.getId())
                        .payload("{\"tenantCertDefinitionId\":1,\"completedDate\":\"2026-08-12\"}")
                        .build();
        service.attachCertificationAttachmentsToWorkflow(withoutAttachments);
        verify(attachmentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void deleteCertificationProofAttachmentsRemovesWorkflowFilesFromStorage() {
        Workflow workflow =
                Workflow.builder()
                        .id(55L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.CERTIFICATION_RECORD.getId())
                        .payload("{\"tenantCertDefinitionId\":1,\"completedDate\":\"2026-08-12\"}")
                        .build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(90L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW)
                        .entityId(55L)
                        .storageKey("tenants/7/workflows/55/proof.pdf")
                        .fileName("proof.pdf")
                        .build();
        EntityAttachment commentAttachment =
                EntityAttachment.builder()
                        .id(91L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT)
                        .entityId(301L)
                        .storageKey("tenants/7/workflows/55/comments/301/scan.pdf")
                        .fileName("scan.pdf")
                        .build();
        EntityAttachment commentDraft =
                EntityAttachment.builder()
                        .id(92L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT_DRAFT)
                        .entityId(55L)
                        .storageKey("tenants/7/workflows/55/comment-drafts/users/1/draft.pdf")
                        .fileName("draft.pdf")
                        .build();
        when(attachmentRepository.findByEntityTypeAndEntityId(EntityAttachmentType.WORKFLOW, 55L))
                .thenReturn(List.of(attachment));
        when(attachmentRepository.findByEntityTypeAndEntityId(
                        EntityAttachmentType.WORKFLOW_COMMENT_DRAFT, 55L))
                .thenReturn(List.of(commentDraft));
        when(commentRepository.findByWorkflow_IdOrderByIdAsc(55L))
                .thenReturn(List.of(WorkflowComment.builder().id(301L).build()));
        when(attachmentRepository.findByEntityTypeAndEntityId(
                        EntityAttachmentType.WORKFLOW_COMMENT, 301L))
                .thenReturn(List.of(commentAttachment));

        service.deleteCertificationProofAttachments(workflow);

        verify(attachmentRepository).delete(attachment);
        verify(attachmentRepository).delete(commentAttachment);
        verify(attachmentRepository).delete(commentDraft);
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "workflows", "tenants/7/workflows/55/proof.pdf");
        verify(deletionOutboxService)
                .enqueueObjectDeletion(
                        7L, "workflows", "tenants/7/workflows/55/comments/301/scan.pdf");
        verify(deletionOutboxService)
                .enqueueObjectDeletion(
                        7L, "workflows", "tenants/7/workflows/55/comment-drafts/users/1/draft.pdf");
    }

    @Test
    void payloadReadersRejectMissingAndInvalidJson() {
        Workflow missingPayload =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .payload(" ")
                        .build();
        assertThatThrownBy(() -> service.attachNameChangeAttachmentsToWorkflow(missingPayload))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("payload missing");

        Workflow invalidPayload =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.ADDRESS_CHANGE.getId())
                        .payload("{bad-json")
                        .build();
        assertThatThrownBy(() -> service.attachAddressChangeAttachmentsToWorkflow(invalidPayload))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Invalid workflow payload");

        Workflow emptyNameChangeAttachments =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                        .payload("{\"firstName\":\"A\",\"lastName\":\"B\",\"attachmentIds\":[]}")
                        .build();
        assertThatThrownBy(
                        () ->
                                service.attachNameChangeAttachmentsToWorkflow(
                                        emptyNameChangeAttachments))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("At least one attachment");
    }

    @Test
    void downloadsCommentAttachmentWhenCommentBelongsToWorkflow() throws Exception {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();
        WorkflowComment comment = WorkflowComment.builder().id(30L).workflow(workflow).build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT)
                        .entityId(30L)
                        .fileName("comment.png")
                        .contentType("image/png")
                        .storageKey("comment-key")
                        .build();
        when(workflowRepository.findWithDetailsById(20L)).thenReturn(Optional.of(workflow));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));
        when(storageService.downloadFile("workflows", "comment-key"))
                .thenReturn(new ByteArrayInputStream("png".getBytes()));

        var bundle = service.downloadWorkflowAttachment(20L, 5L);

        assertThat(bundle.fileName()).isEqualTo("comment.png");
        assertThat(bundle.resource().getInputStream().readAllBytes()).isEqualTo("png".getBytes());
    }

    @Test
    void downloadRejectsCommentAttachmentFromAnotherWorkflow() {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();
        Workflow otherWorkflow = Workflow.builder().id(99L).tenantId(7L).build();
        WorkflowComment comment = WorkflowComment.builder().id(30L).workflow(otherWorkflow).build();
        EntityAttachment attachment =
                EntityAttachment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.WORKFLOW_COMMENT)
                        .entityId(30L)
                        .build();
        when(workflowRepository.findWithDetailsById(20L)).thenReturn(Optional.of(workflow));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.downloadWorkflowAttachment(20L, 5L))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void downloadsPayloadDraftAttachmentForOpenAddressChangeWorkflow() throws Exception {
        Workflow workflow =
                Workflow.builder()
                        .id(20L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .workflowTypeId(WorkflowType.ADDRESS_CHANGE.getId())
                        .status(WorkflowStatus.OPEN)
                        .payload(
                                "{\"street\":\"New\",\"city\":\"City\",\"postalCode\":\"1\",\"country\":\"DE\",\"attachmentIds\":[5]}")
                        .build();
        EntityAttachment draft = draft(5L);
        when(workflowRepository.findWithDetailsById(20L)).thenReturn(Optional.of(workflow));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(storageService.downloadFile("workflows", "tenants/7/workflows/drafts/draft-key"))
                .thenReturn(new ByteArrayInputStream("data".getBytes()));

        var bundle = service.downloadWorkflowAttachment(20L, 5L);

        assertThat(bundle.fileName()).isEqualTo("proof.pdf");
        assertThat(bundle.resource().getInputStream().readAllBytes()).isEqualTo("data".getBytes());
    }

    @Test
    void listsFirstAttachmentForEachRequestedComment() {
        Workflow workflow = Workflow.builder().id(20L).tenantId(7L).build();
        EntityAttachment first =
                EntityAttachment.builder()
                        .id(5L)
                        .fileName("first.png")
                        .contentType("image/png")
                        .sizeBytes(1L)
                        .uploadedBy(1L)
                        .build();
        EntityAttachment second =
                EntityAttachment.builder()
                        .id(6L)
                        .fileName("second.png")
                        .contentType("image/png")
                        .sizeBytes(1L)
                        .uploadedBy(1L)
                        .build();
        when(attachmentRepository.findByEntityTypeAndEntityId(
                        EntityAttachmentType.WORKFLOW_COMMENT, 30L))
                .thenReturn(List.of(first, second));

        Map<Long, EntityAttachmentDto> result =
                service.listCommentAttachments(workflow, List.of(30L, 31L));

        assertThat(result).containsOnlyKeys(30L);
        assertThat(result.get(30L).getId()).isEqualTo(5L);
    }

    private EntityAttachment draft(long id) {
        return EntityAttachment.builder()
                .id(id)
                .tenantId(7L)
                .entityType(EntityAttachmentType.WORKFLOW_DRAFT)
                .entityId(1L)
                .organizationId(10L)
                .fileName("proof.pdf")
                .contentType("application/pdf")
                .sizeBytes(4L)
                .storageKey("tenants/7/workflows/drafts/draft-key")
                .uploadedBy(1L)
                .build();
    }
}
