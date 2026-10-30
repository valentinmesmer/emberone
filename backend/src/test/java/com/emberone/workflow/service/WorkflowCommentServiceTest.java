/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.security.WorkflowInstanceAccess;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.dto.CreateWorkflowCommentRequest;
import com.emberone.workflow.event.WorkflowEventPublisher;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.exception.WorkflowAccessDeniedException;
import com.emberone.workflow.exception.WorkflowStateException;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowCommentRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowCommentServiceTest {

    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowCommentRepository commentRepository;
    @Mock WorkflowInstanceAccess workflowInstanceAccess;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock UserRepository userRepository;
    @Mock UserOrganizationGroupMembershipRepository membershipRepository;
    @Mock TokenService tokenService;
    @Mock WorkflowEventPublisher eventPublisher;
    private WorkflowCommentService service;

    @BeforeEach
    void setUp() {
        service =
                new WorkflowCommentService(
                        workflowRepository,
                        commentRepository,
                        workflowInstanceAccess,
                        workflowDocumentService,
                        userRepository,
                        membershipRepository,
                        tokenService,
                        eventPublisher);
    }

    @Test
    void openerCanCreateTrimmedCommentAndAttachDraft() {
        Workflow workflow = workflow(1L);
        arrangeComment(workflow, 1L);
        when(commentRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            WorkflowComment comment = invocation.getArgument(0);
                            comment.setId(10L);
                            return comment;
                        });
        when(userRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .id(1L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .build()));

        var dto =
                service.createComment(
                        5L,
                        CreateWorkflowCommentRequest.builder()
                                .body(" hello ")
                                .attachmentId(20L)
                                .build());

        assertThat(dto.getBody()).isEqualTo("hello");
        assertThat(dto.getAuthorRole()).isEqualTo(WorkflowCommentAuthorRole.OPENER);
        verify(workflowDocumentService)
                .attachCommentDraftToComment(
                        org.mockito.ArgumentMatchers.eq(workflow),
                        any(),
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(1L));
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.COMMENT_CREATED);
    }

    @Test
    void activeOrganizationGroupApproverGetsApproverRole() {
        Workflow workflow = workflow(99L);
        workflow.getParticipants()
                .add(
                        WorkflowParticipant.builder()
                                .role(ParticipantRole.APPROVER)
                                .requirement(ParticipantRequirement.MANDATORY)
                                .principalType(PrincipalType.ORGANIZATION_GROUP)
                                .principalId(30L)
                                .organizationId(10L)
                                .build());
        arrangeComment(workflow, 2L);
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(2L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        any(Instant.class)))
                .thenReturn(List.of(30L));
        when(commentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).build()));

        assertThat(
                        service.createComment(
                                        5L,
                                        CreateWorkflowCommentRequest.builder().body("ok").build())
                                .getAuthorRole())
                .isEqualTo(WorkflowCommentAuthorRole.APPROVER);
    }

    @Test
    void deniesCommentWithoutAccessAndOnClosedWorkflow() {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(workflowInstanceAccess.canComment(5L, 1L)).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                service.createComment(
                                        5L,
                                        CreateWorkflowCommentRequest.builder()
                                                .body("test")
                                                .build()))
                .isInstanceOf(WorkflowAccessDeniedException.class);

        Workflow workflow = workflow(1L);
        workflow.setStatus(WorkflowStatus.APPROVED);
        when(workflowInstanceAccess.canComment(5L, 1L)).thenReturn(true);
        when(workflowRepository.findWithDetailsById(5L)).thenReturn(Optional.of(workflow));
        assertThatThrownBy(
                        () ->
                                service.createComment(
                                        5L,
                                        CreateWorkflowCommentRequest.builder()
                                                .body("test")
                                                .build()))
                .isInstanceOf(WorkflowStateException.class);
    }

    @Test
    void emptyCommentIsRejectedBeforePersistence() {
        Workflow workflow = workflow(1L);
        arrangeComment(workflow, 1L);

        assertThatThrownBy(
                        () ->
                                service.createComment(
                                        5L,
                                        CreateWorkflowCommentRequest.builder().body(" ").build()))
                .isInstanceOf(WorkflowValidationException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void listCommentsRequiresViewAndEnrichesAuthorsAndAttachments() {
        Workflow workflow = workflow(1L);
        WorkflowComment comment =
                WorkflowComment.builder()
                        .id(10L)
                        .workflow(workflow)
                        .userId(1L)
                        .authorRole(WorkflowCommentAuthorRole.OPENER)
                        .body("hello")
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(workflowInstanceAccess.canView(5L, 1L)).thenReturn(true);
        when(workflowRepository.findWithDetailsById(5L)).thenReturn(Optional.of(workflow));
        when(commentRepository.findByWorkflow_IdOrderByIdAsc(5L)).thenReturn(List.of(comment));
        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(
                                User.builder()
                                        .id(1L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .build()));
        when(workflowDocumentService.listCommentAttachments(workflow, List.of(10L)))
                .thenReturn(Map.of());

        assertThat(service.listComments(5L))
                .singleElement()
                .satisfies(
                        dto -> {
                            assertThat(dto.getUserDisplayName()).isEqualTo("Ada Lovelace");
                            assertThat(dto.getBody()).isEqualTo("hello");
                        });
    }

    private void arrangeComment(Workflow workflow, long userId) {
        when(tokenService.requireUserId()).thenReturn(userId);
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(workflowInstanceAccess.canComment(5L, userId)).thenReturn(true);
        when(workflowRepository.findWithDetailsById(5L)).thenReturn(Optional.of(workflow));
    }

    private Workflow workflow(long openerId) {
        return Workflow.builder()
                .id(5L)
                .tenantId(7L)
                .openedByUserId(openerId)
                .status(WorkflowStatus.OPEN)
                .participants(new LinkedHashSet<>())
                .build();
    }
}
