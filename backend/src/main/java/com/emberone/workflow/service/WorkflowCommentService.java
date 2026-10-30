/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.security.WorkflowInstanceAccess;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.dto.CreateWorkflowCommentRequest;
import com.emberone.workflow.dto.WorkflowCommentDto;
import com.emberone.workflow.event.WorkflowEventPublisher;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.exception.WorkflowAccessDeniedException;
import com.emberone.workflow.exception.WorkflowNotFoundException;
import com.emberone.workflow.exception.WorkflowStateException;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowCommentRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WorkflowCommentService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowCommentRepository commentRepository;
    private final WorkflowInstanceAccess workflowInstanceAccess;
    private final WorkflowDocumentService workflowDocumentService;
    private final UserRepository userRepository;
    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final TokenService tokenService;
    private final WorkflowEventPublisher eventPublisher;

    private static String formatUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? null : combined;
    }

    @Transactional(readOnly = true)
    public List<WorkflowCommentDto> listComments(Long workflowId) {
        Long userId = tokenService.requireUserId();
        ensureCanView(workflowId, userId);

        Workflow workflow =
                workflowRepository
                        .findWithDetailsById(workflowId)
                        .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        List<WorkflowComment> comments =
                commentRepository.findByWorkflow_IdOrderByIdAsc(workflowId);
        if (comments.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds =
                comments.stream().map(WorkflowComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> usersById =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> commentIds = comments.stream().map(WorkflowComment::getId).toList();
        Map<Long, EntityAttachmentDto> attachmentByCommentId =
                workflowDocumentService.listCommentAttachments(workflow, commentIds);

        List<WorkflowCommentDto> result = new ArrayList<>(comments.size());
        for (WorkflowComment comment : comments) {
            User author = usersById.get(comment.getUserId());
            result.add(toDto(comment, author, attachmentByCommentId.get(comment.getId())));
        }
        return result;
    }

    @Transactional
    public WorkflowCommentDto createComment(Long workflowId, CreateWorkflowCommentRequest request) {
        Long userId = tokenService.requireUserId();
        Long tenantId = tokenService.requireTenantId();
        ensureCanComment(workflowId, userId);

        Workflow workflow =
                workflowRepository
                        .findWithDetailsById(workflowId)
                        .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        if (!workflow.getStatus().isActive()) {
            throw new WorkflowStateException("Comments are only allowed on active workflows");
        }

        String body = request.getBody() != null ? request.getBody().trim() : "";
        if (body.isEmpty()) {
            throw new WorkflowValidationException("Comment must not be empty");
        }

        WorkflowCommentAuthorRole authorRole = resolveAuthorRole(workflow, userId);
        WorkflowComment comment =
                WorkflowComment.builder()
                        .tenantId(tenantId)
                        .workflow(workflow)
                        .userId(userId)
                        .authorRole(authorRole)
                        .body(body)
                        .build();
        WorkflowComment saved = commentRepository.save(comment);

        EntityAttachmentDto attachment = null;
        if (request.getAttachmentId() != null) {
            attachment =
                    workflowDocumentService.attachCommentDraftToComment(
                            workflow, saved, request.getAttachmentId(), userId);
        }

        User author = userRepository.findById(userId).orElseThrow();
        eventPublisher.publish(workflow, WorkflowLifecycleEvent.COMMENT_CREATED);
        return toDto(saved, author, attachment);
    }

    @Transactional
    public EntityAttachmentDto uploadCommentAttachmentDraft(Long workflowId, MultipartFile file) {
        Long userId = tokenService.requireUserId();
        ensureCanComment(workflowId, userId);

        Workflow workflow =
                workflowRepository
                        .findWithDetailsById(workflowId)
                        .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        if (!workflow.getStatus().isActive()) {
            throw new WorkflowStateException(
                    "Comment attachments are only allowed on active workflows");
        }
        return workflowDocumentService.uploadWorkflowCommentAttachmentDraft(workflow, userId, file);
    }

    private void ensureCanView(Long workflowId, Long userId) {
        if (!workflowInstanceAccess.canView(workflowId, userId)) {
            throw new WorkflowAccessDeniedException("You cannot view this workflow");
        }
    }

    private void ensureCanComment(Long workflowId, Long userId) {
        if (!workflowInstanceAccess.canComment(workflowId, userId)) {
            throw new WorkflowAccessDeniedException("You cannot comment on this workflow");
        }
    }

    private WorkflowCommentAuthorRole resolveAuthorRole(Workflow workflow, Long userId) {
        if (userId.equals(workflow.getOpenedByUserId())) {
            return WorkflowCommentAuthorRole.OPENER;
        }
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (participant.getRole() == ParticipantRole.VIEWER
                    && participant.getPrincipalType() == PrincipalType.USER
                    && userId.equals(participant.getPrincipalId())) {
                return WorkflowCommentAuthorRole.VIEWER;
            }
        }
        Instant now = Instant.now();
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (relatesToApproverSlot(participant, userId, now)) {
                return WorkflowCommentAuthorRole.APPROVER;
            }
        }
        throw new WorkflowAccessDeniedException("You cannot comment on this workflow");
    }

    private boolean relatesToApproverSlot(
            WorkflowParticipant participant, Long userId, Instant at) {
        if (!participant.isApprover()) {
            return false;
        }
        if (participant.getPrincipalType() == PrincipalType.USER) {
            return userId.equals(participant.getPrincipalId());
        }
        if (participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP) {
            Long organizationId = participant.getOrganizationId();
            Long groupId = participant.getPrincipalId();
            if (groupId == null || organizationId == null) {
                return false;
            }
            return membershipRepository
                    .findActiveGroupIdsForUserInOrganization(userId, organizationId, at)
                    .contains(groupId);
        }
        return false;
    }

    private WorkflowCommentDto toDto(
            WorkflowComment comment, User author, EntityAttachmentDto attachment) {
        return WorkflowCommentDto.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .userFirstName(author != null ? author.getFirstName() : null)
                .userLastName(author != null ? author.getLastName() : null)
                .userDisplayName(formatUserDisplayName(author))
                .authorRole(comment.getAuthorRole())
                .body(comment.getBody())
                .attachment(attachment)
                .build();
    }
}
