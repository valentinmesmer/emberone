/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.security.WorkflowInstanceAccess;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.dto.*;
import com.emberone.workflow.event.*;
import com.emberone.workflow.exception.*;
import com.emberone.workflow.list.WorkflowListDefinition;
import com.emberone.workflow.list.WorkflowListFilter;
import com.emberone.workflow.list.WorkflowListSearch;
import com.emberone.workflow.list.WorkflowListSpecs;
import com.emberone.workflow.mapper.WorkflowMapper;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.spi.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    public static final int DEFAULT_WORKFLOW_PAGE_SIZE = 50;

    private final WorkflowHandlerRegistry handlerRegistry;
    private final WorkflowDefinitionService definitionService;
    private final WorkflowTypeMappingService mappingService;
    private final WorkflowSelfApproveService selfApproveService;
    private final WorkflowResolverService resolverService;
    private final WorkflowRepository workflowRepository;
    private final WorkflowListSearch workflowListSearch;
    private final WorkflowParticipantRepository participantRepository;
    private final WorkflowMapper mapper;
    private final WorkflowEventPublisher eventPublisher;
    private final TokenService tokenService;
    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;
    private final Validator validator;
    private final OrganizationAccessService organizationAccessService;
    private final WorkflowInstanceAccess workflowInstanceAccess;
    private final WorkflowDocumentPromotionService documentPromotionService;
    private final WorkflowParticipantClaimService participantClaimService;
    private final WorkflowOpenConcurrencyService openConcurrencyService;
    private final WorkflowOpenExclusivityService openExclusivityService;

    private static String formatUserDisplayName(User user) {
        if (user == null) {
            return "";
        }
        String name = formatUserDisplayName(user.getFirstName(), user.getLastName());
        if (!name.isEmpty()) {
            return name;
        }
        return user.getUsername() != null ? user.getUsername().trim() : "";
    }

    private static String formatUserDisplayName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""))
                .trim();
    }

    private static boolean hasPendingApproverSlots(Workflow workflow) {
        return workflow.getParticipants().stream()
                .anyMatch(
                        participant ->
                                participant.isApprover()
                                        && participant.getSlotStatus()
                                                == ParticipantSlotStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public WorkflowDocumentPromotionPreviewDto getDocumentPromotionPreview(Long workflowId) {
        Workflow workflow = requireWorkflowWithDetails(workflowId);
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        return documentPromotionService.buildPreview(workflow, handler);
    }

    @Transactional
    public WorkflowDocumentPromotionPreviewDto updateDocumentPromotionSelections(
            Long workflowId, UpdateWorkflowDocumentPromotionsRequest request) {
        Workflow workflow = requireOpenWorkflow(workflowId);
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        WorkflowDocumentPromotionPreviewDto preview =
                documentPromotionService.savePendingSelections(
                        workflow, handler, request.getSelections());
        workflowRepository.save(workflow);
        eventPublisher.publish(workflow, WorkflowLifecycleEvent.UPDATED);
        return preview;
    }

    /**
     * Opens a workflow instance for the given type. Called by the business endpoint of the concrete
     * workflow, not by a generic create endpoint.
     */
    @Transactional
    public WorkflowDto open(
            Long workflowTypeId, Object payload, String subjectEntityType, Long subjectEntityId) {
        return openForUser(
                tokenService.requireUserId(),
                workflowTypeId,
                payload,
                subjectEntityType,
                subjectEntityId);
    }

    @Transactional
    public WorkflowDto openForUser(
            Long openedByUserId,
            Long workflowTypeId,
            Object payload,
            String subjectEntityType,
            Long subjectEntityId) {
        Long tenantId = tokenService.requireTenantId();

        WorkflowHandler<?> handler = handlerRegistry.require(workflowTypeId);
        if (!userOrganizationMembershipRepository.existsActiveByUser_Id(
                        openedByUserId, Instant.now())
                && !handler.mayOpenWithoutActiveOrganizationMembership()) {
            throw new WorkflowValidationException(
                    "No organization membership - this workflow cannot be opened");
        }
        if (handler.requiresOpenerOrganizationMembership()
                && userOrganizationMembershipRepository
                        .findActiveByUser_IdAndMainTrue(openedByUserId, Instant.now())
                        .isEmpty()) {
            throw new WorkflowValidationException(
                    "No main organization membership - this workflow cannot be opened");
        }
        if (!handler.allowsMultipleConcurrentOpen()
                && workflowRepository.existsActiveByOpenedByUserIdAndWorkflowTypeId(
                        openedByUserId, workflowTypeId)) {
            throw new WorkflowValidationException(
                    "You already have an open workflow of this type; finish or cancel it before opening another",
                    WorkflowValidationException.CODE_TYPE_ALREADY_OPEN);
        }
        OpenResolution resolution = invokeHandlerOpen(handler, openedByUserId, tenantId, payload);

        WorkflowDefinition definition =
                definitionService.resolveEffectiveDefinition(
                        workflowTypeId, resolution.contextOrgId());
        if (!definition.isEnabled()) {
            throw new WorkflowStateException(
                    "Workflow type id "
                            + workflowTypeId
                            + " is disabled for organization "
                            + resolution.contextOrgId());
        }
        List<Long> staticOrgIds =
                definition.getStaticOrganizations().stream()
                        .map(WorkflowDefinitionStaticOrg::getApproverOrganizationId)
                        .toList();

        Workflow workflow =
                Workflow.builder()
                        .tenantId(tenantId)
                        .workflowTypeId(workflowTypeId)
                        .status(WorkflowStatus.OPEN)
                        .openedAt(Instant.now())
                        .openedByUserId(openedByUserId)
                        .subjectEntityType(subjectEntityType)
                        .subjectEntityId(subjectEntityId)
                        .payload(serializePayload(payload))
                        .openConcurrencyKeys(
                                openConcurrencyService.serializeKeys(
                                        resolution.openConcurrencyKeys()))
                        .build();

        resolverService.resolveParticipants(
                workflow,
                openedByUserId,
                workflowTypeId,
                resolution.contextOrgId(),
                staticOrgIds,
                definition.isStaticApproversExclusive(),
                definition.isSelfApproveAllowed(),
                resolution.dynamicOrgIds(),
                resolution.additional());

        Workflow saved = workflowRepository.save(workflow);
        openExclusivityService.replaceLocks(
                handler, tenantId, saved.getId(), resolution.openExclusivityKeys());
        recordAutoApproveDecisions(saved);
        invokeHandlerOpened(handler, saved);

        if (hasPendingApproverSlots(saved)) {
            eventPublisher.publish(saved, WorkflowLifecycleEvent.OPENED);
        }
        completeIfAllApproved(saved, handler);

        log.info("Opened workflow {} of type id {}", saved.getId(), workflowTypeId);
        eventPublisher.publish(saved, WorkflowLifecycleEvent.UPDATED);
        return toEnrichedDto(saved);
    }

    @Transactional(readOnly = true)
    public WorkflowDto getWorkflow(Long workflowId) {
        return toEnrichedDto(requireWorkflowWithDetails(workflowId));
    }

    @Transactional(readOnly = true)
    public WorkflowPageDto listOpenedForCurrentUser(ListQuery query, WorkflowListFilter filter) {
        Long userId = tokenService.requireUserId();
        return listWorkflowPage(query, filter, WorkflowListSpecs.openedByUser(userId));
    }

    @Transactional(readOnly = true)
    public List<Long> listDistinctInboxWorkflowTypeIds() {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        java.util.Set<Long> typeIds = new java.util.TreeSet<>();
        typeIds.addAll(workflowRepository.findDistinctWorkflowTypeIdsByOpenedByUserId(userId));
        List<Long> effectiveGroupIds = groupIds.isEmpty() ? List.of(-1L) : groupIds;
        typeIds.addAll(
                workflowRepository.findDistinctWorkflowTypeIdsForParticipantCandidates(
                        userId, effectiveGroupIds));
        return List.copyOf(typeIds);
    }

    @Transactional(readOnly = true)
    public List<Long> listDistinctWorkflowTypeIdsOpenedByUserInOrganization(
            Long userId, Long organizationId) {
        return workflowRepository.findDistinctWorkflowTypeIdsByOpenedByUserIdInOrganization(
                userId, organizationId);
    }

    @Transactional(readOnly = true)
    public List<Long> listDistinctWorkflowTypeIdsForOrganization(Long organizationId) {
        return workflowRepository.findDistinctWorkflowTypeIdsForOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public WorkflowPageDto listParticipationsForCurrentUser(
            ListQuery query, WorkflowListFilter filter) {
        Long userId = tokenService.requireUserId();
        List<Long> groupIds = activeGroupIds(userId);
        List<Long> candidateIds =
                workflowRepository
                        .findAll(
                                Specification.allOf(
                                        WorkflowListSpecs.participantCandidateForUser(
                                                userId, groupIds),
                                        WorkflowListSpecs.workflowStatusesFilter(filter.statuses()),
                                        WorkflowListSpecs.workflowTypesFilter(
                                                filter.workflowTypeIds()),
                                        workflowListSearch.search(query)),
                                WorkflowListDefinition.INSTANCE.pageable(query).getSort())
                        .stream()
                        .map(Workflow::getId)
                        .toList();
        if (candidateIds.isEmpty()) {
            return emptyWorkflowPage(query);
        }
        List<Workflow> hydrated = workflowRepository.findWithDetailsByIdIn(candidateIds);
        List<Workflow> ordered = PageReorder.reorderByIds(candidateIds, hydrated, Workflow::getId);
        List<Workflow> filtered =
                ordered.stream()
                        .filter(
                                workflow ->
                                        workflowInstanceAccess.requiresParticipantAction(
                                                workflow, userId))
                        .toList();
        return sliceToWorkflowPage(query, filtered);
    }

    @Transactional(readOnly = true)
    public long countPendingForCurrentUser() {
        Long userId = tokenService.requireUserId();
        Set<Long> pendingIds = new LinkedHashSet<>();
        Specification<Workflow> openedActive =
                Specification.allOf(
                        WorkflowListSpecs.openedByUser(userId),
                        WorkflowListSpecs.workflowStatusesFilter(
                                List.of(
                                        WorkflowStatus.OPEN,
                                        WorkflowStatus.AWAITING_INPUT_CHANGE)));
        for (Workflow workflow : workflowRepository.findAll(openedActive)) {
            if (workflow.getStatus().isActive()) {
                pendingIds.add(workflow.getId());
            }
        }
        List<Long> groupIds = activeGroupIds(userId);
        List<Long> participationIds =
                workflowRepository
                        .findAll(
                                Specification.allOf(
                                        WorkflowListSpecs.participantCandidateForUser(
                                                userId, groupIds),
                                        WorkflowListSpecs.workflowStatusesFilter(
                                                List.of(
                                                        WorkflowStatus.OPEN,
                                                        WorkflowStatus.AWAITING_INPUT_CHANGE))))
                        .stream()
                        .map(Workflow::getId)
                        .toList();
        if (!participationIds.isEmpty()) {
            List<Workflow> participations =
                    workflowRepository.findWithDetailsByIdIn(participationIds);
            for (Workflow workflow : participations) {
                if (workflowInstanceAccess.requiresParticipantAction(workflow, userId)
                        && workflow.getStatus() == WorkflowStatus.OPEN) {
                    pendingIds.add(workflow.getId());
                }
            }
        }
        return pendingIds.size();
    }

    @Transactional(readOnly = true)
    public WorkflowOpenStateDto getOpenStateForCurrentUser(long workflowTypeId) {
        Long userId = tokenService.requireUserId();
        handlerRegistry.require(workflowTypeId);
        return buildOpenState(userId, workflowTypeId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowOpenConcurrencyKeysDto> getOpenConcurrencyKeysForCurrentUser(
            Long workflowTypeId) {
        Long userId = tokenService.requireUserId();
        if (workflowTypeId != null) {
            handlerRegistry.require(workflowTypeId);
            return List.of(
                    WorkflowOpenConcurrencyKeysDto.builder()
                            .workflowTypeId(workflowTypeId)
                            .keys(
                                    List.copyOf(
                                            openConcurrencyService.findOpenKeys(
                                                    userId, workflowTypeId)))
                            .build());
        }
        return handlerRegistry.all().stream()
                .map(
                        handler ->
                                WorkflowOpenConcurrencyKeysDto.builder()
                                        .workflowTypeId(handler.workflowTypeId())
                                        .keys(
                                                List.copyOf(
                                                        openConcurrencyService.findOpenKeys(
                                                                userId, handler.workflowTypeId())))
                                        .build())
                .filter(dto -> !dto.getKeys().isEmpty())
                .sorted(Comparator.comparingLong(WorkflowOpenConcurrencyKeysDto::getWorkflowTypeId))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowPageDto listOpenedByUserInOrganization(
            Long userId, Long organizationId, ListQuery query, WorkflowListFilter filter) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return listWorkflowPage(
                query,
                filter,
                WorkflowListSpecs.openedByUserInOrganization(userId, organizationId));
    }

    /** Lists workflows routed through the given organization (approver context on participants). */
    @Transactional(readOnly = true)
    public WorkflowPageDto listForOrganization(
            Long organizationId, ListQuery query, WorkflowListFilter filter) {
        Long tenantId = tokenService.requireTenantId();
        if (organizationAccessService.organizationNotInTenant(organizationId, tenantId)) {
            return emptyWorkflowPage(query);
        }
        return listWorkflowPage(
                query, filter, WorkflowListSpecs.routedThroughOrganization(organizationId));
    }

    private WorkflowPageDto listWorkflowPage(
            ListQuery query, WorkflowListFilter filter, Specification<Workflow> scope) {
        Pageable pageable = WorkflowListDefinition.INSTANCE.pageable(query);
        Specification<Workflow> spec =
                Specification.allOf(
                        scope,
                        WorkflowListSpecs.workflowStatusesFilter(filter.statuses()),
                        WorkflowListSpecs.workflowTypesFilter(filter.workflowTypeIds()),
                        workflowListSearch.search(query));
        Page<Workflow> entityPage = workflowRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(Workflow::getId).toList();
        List<Workflow> hydrated =
                ids.isEmpty() ? List.of() : workflowRepository.findWithDetailsByIdIn(ids);
        List<Workflow> ordered = PageReorder.reorderByIds(ids, hydrated, Workflow::getId);
        Page<Workflow> page = PageReorder.pageOf(ordered, entityPage);
        return toWorkflowPageDto(page);
    }

    private WorkflowPageDto sliceToWorkflowPage(ListQuery query, List<Workflow> workflows) {
        Pageable pageable = WorkflowListDefinition.INSTANCE.pageable(query);
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        long total = workflows.size();
        int from = Math.min(page * size, workflows.size());
        int to = Math.min(from + size, workflows.size());
        List<Workflow> slice = workflows.subList(from, to);
        List<WorkflowDto> content = toEnrichedDtoList(slice);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return WorkflowPageDto.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .build();
    }

    private WorkflowPageDto emptyWorkflowPage(ListQuery query) {
        Pageable pageable = WorkflowListDefinition.INSTANCE.pageable(query);
        return WorkflowPageDto.builder()
                .content(List.of())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(0)
                .totalPages(0)
                .hasNext(false)
                .build();
    }

    private WorkflowPageDto toWorkflowPageDto(Page<Workflow> page) {
        List<WorkflowDto> content = toEnrichedDtoList(page.getContent());
        return WorkflowPageDto.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional
    public WorkflowDto claim(Long workflowId, Long participantId) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireOpenWorkflow(workflowId);
        WorkflowParticipant participant = requireParticipant(workflowId, participantId);

        if (!participant.isApprover()
                || participant.getPrincipalType() != PrincipalType.ORGANIZATION_GROUP) {
            throw new WorkflowStateException(
                    "Only organization-group approver slots can be claimed");
        }
        if (participant.getClaimedByUserId() != null) {
            throw new WorkflowStateException("Slot is already claimed");
        }
        if (participant.getSlotStatus() != ParticipantSlotStatus.PENDING) {
            throw new WorkflowStateException("Slot is not pending");
        }
        if (!isActiveGroupMember(
                userId, participant.getOrganizationId(), participant.getPrincipalId())) {
            throw new WorkflowAccessDeniedException(
                    "User is not an active member of the slot's organization group");
        }

        participant.setClaimedByUserId(userId);
        participant.setClaimedAt(Instant.now());
        participantRepository.save(participant);

        log.info("Claimed participant {} on workflow {}", participantId, workflowId);
        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.CLAIMED,
                WorkflowEventContext.ofActor(participant.getId(), userId));
        return toEnrichedDto(updated);
    }

    @Transactional
    public WorkflowDto unclaim(Long workflowId, Long participantId) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireActiveWorkflow(workflowId);
        WorkflowParticipant participant = requireParticipant(workflowId, participantId);

        if (!participant.isApprover()
                || participant.getPrincipalType() != PrincipalType.ORGANIZATION_GROUP) {
            throw new WorkflowStateException(
                    "Only organization-group approver slots can be unclaimed");
        }
        if (participant.getClaimedByUserId() == null) {
            throw new WorkflowStateException("Slot is not claimed");
        }
        if (!participant.getClaimedByUserId().equals(userId)) {
            throw new WorkflowAccessDeniedException("Only the slot's claimer may unclaim");
        }
        if (participant.getSlotStatus() != ParticipantSlotStatus.PENDING) {
            throw new WorkflowStateException("Slot is not pending");
        }

        participantClaimService.releasePendingClaim(participant, userId);

        log.info("Unclaimed participant {} on workflow {}", participantId, workflowId);
        return toEnrichedDto(requireWorkflowWithDetails(workflowId));
    }

    @Transactional
    public WorkflowDto withdrawApproval(Long workflowId, Long participantId) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireOpenWorkflow(workflowId);
        WorkflowParticipant participant = requireParticipant(workflowId, participantId);

        if (!participant.isApprover()) {
            throw new WorkflowStateException("Target participant is not an approver slot");
        }
        if (participant.getSlotStatus() != ParticipantSlotStatus.APPROVED) {
            throw new WorkflowStateException("Slot is not approved");
        }
        WorkflowDecision decision =
                workflow.getDecisions().stream()
                        .filter(d -> participant.getId().equals(d.getParticipantId()))
                        .filter(d -> d.getDecisionType() == DecisionType.APPROVED)
                        .filter(d -> userId.equals(d.getUserId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new WorkflowAccessDeniedException(
                                                "Only the approver who granted approval may withdraw it"));
        if (!userId.equals(participant.getClaimedByUserId())) {
            throw new WorkflowStateException("Slot must still be claimed by the withdrawing user");
        }

        workflow.getDecisions().remove(decision);
        participant.setSlotStatus(ParticipantSlotStatus.PENDING);
        participantRepository.save(participant);
        workflowRepository.save(workflow);

        log.info("Withdrew approval on participant {} for workflow {}", participantId, workflowId);
        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.APPROVAL_WITHDRAWN,
                WorkflowEventContext.ofActor(participant.getId(), userId));
        return toEnrichedDto(updated);
    }

    @Transactional
    public WorkflowDto decide(Long workflowId, WorkflowDecisionRequest request) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireOpenWorkflow(workflowId);
        WorkflowParticipant participant =
                requireParticipant(workflowId, request.getParticipantId());
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());

        if (!participant.isApprover()) {
            throw new WorkflowStateException("Target participant is not an approver slot");
        }
        if (participant.getSlotStatus() != ParticipantSlotStatus.PENDING) {
            throw new WorkflowStateException("Slot is not pending");
        }
        if (participant.getClaimedByUserId() == null) {
            throw new WorkflowStateException("Slot must be claimed before a decision can be made");
        }
        if (!participant.getClaimedByUserId().equals(userId)) {
            throw new WorkflowAccessDeniedException("Only the slot's claimer may decide");
        }

        if (request.getDecision() == DecisionType.REJECTED && handler.mapsRejectToInputChange()) {
            assertMayRequestInputChange(workflow, userId);
            applyInputChangeRequest(workflow, userId, request.getComment(), handler);
            log.info(
                    "Mapped reject to input change on workflow {} (participant {})",
                    workflowId,
                    participant.getId());
            Workflow updated = requireWorkflowWithDetails(workflowId);
            eventPublisher.publish(
                    updated,
                    WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED,
                    WorkflowEventContext.ofActor(participant.getId(), userId));
            eventPublisher.publish(updated, WorkflowLifecycleEvent.UPDATED);
            return toEnrichedDto(updated);
        }

        WorkflowDecision decision =
                WorkflowDecision.builder()
                        .tenantId(workflow.getTenantId())
                        .workflow(workflow)
                        .participantId(participant.getId())
                        .userId(userId)
                        .decisionType(request.getDecision())
                        .comment(request.getComment())
                        .decidedAt(Instant.now())
                        .build();
        workflow.getDecisions().add(decision);

        if (request.getDecision() == DecisionType.REJECTED) {
            participant.setSlotStatus(ParticipantSlotStatus.REJECTED);
            workflow.setStatus(WorkflowStatus.REJECTED);
            workflow.setDecidedAt(Instant.now());
            workflowRepository.save(workflow);
            openExclusivityService.releaseLocks(workflow.getId());
            handler.onRejected(workflow);
            eventPublisher.publish(workflow, WorkflowLifecycleEvent.REJECTED);
            log.info("Rejected workflow {} on participant {}", workflowId, participant.getId());
        } else {
            participant.setSlotStatus(ParticipantSlotStatus.APPROVED);
            workflowRepository.save(workflow);
            completeIfAllApproved(workflow, handler);
            log.info("Approved workflow {} on participant {}", workflowId, participant.getId());
        }

        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.DECISION_RECORDED,
                WorkflowEventContext.ofActor(participant.getId(), userId));
        return toEnrichedDto(updated);
    }

    @Transactional
    public WorkflowDto cancel(Long workflowId) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireActiveWorkflow(workflowId);
        if (!workflow.getOpenedByUserId().equals(userId)) {
            throw new WorkflowAccessDeniedException("Only the opener may cancel the workflow");
        }
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        applyCancellation(workflow, Instant.now(), true, handler, userId);
        log.info("Cancelled workflow {}", workflowId);
        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(updated, WorkflowLifecycleEvent.UPDATED);
        return toEnrichedDto(updated);
    }

    /** Cancels all active workflows opened by a user being deleted (no opener auth check). */
    @Transactional
    public void cancelActiveWorkflowsOpenedByUser(Long userId) {
        if (userId == null) {
            return;
        }
        for (Workflow workflow : workflowRepository.findActiveByOpenedByUserId(userId)) {
            applyCancellation(workflow, Instant.now(), false, null, null);
            log.info(
                    "Cancelled workflow {} because opener user {} was deleted",
                    workflow.getId(),
                    userId);
        }
    }

    /**
     * Cancels active workflows where the user is the subject (e.g. org-initiated user workflows).
     */
    @Transactional
    public void cancelActiveWorkflowsForSubjectUser(Long userId) {
        if (userId == null) {
            return;
        }
        for (Workflow workflow : workflowRepository.findActiveBySubjectEntity("USER", userId)) {
            applyCancellation(workflow, Instant.now(), false, null, null);
            log.info(
                    "Cancelled workflow {} because subject user {} was deactivated",
                    workflow.getId(),
                    userId);
        }
    }

    /**
     * Closes the given workflows that are still active, because the thing they decide on is gone
     * (no opener check, no handler callback, no lifecycle notification: the caller already settled
     * the domain state and sends domain-specific notifications if needed). Workflows that are no
     * longer active are left untouched.
     */
    @Transactional
    public void cancelActiveWorkflows(Collection<Long> workflowIds) {
        if (workflowIds == null || workflowIds.isEmpty()) {
            return;
        }
        for (Workflow workflow : workflowRepository.findAllById(workflowIds)) {
            if (!workflow.getStatus().isActive()) {
                continue;
            }
            applyCancellationWithoutNotification(workflow, Instant.now());
            log.info("Cancelled workflow {} because its subject was closed", workflow.getId());
        }
    }

    private void applyCancellationWithoutNotification(Workflow workflow, Instant at) {
        workflow.setStatus(WorkflowStatus.CANCELLED);
        workflow.setDecidedAt(at);
        cancelOpenInputChanges(workflow, at);
        workflowRepository.save(workflow);
        openExclusivityService.releaseLocks(workflow.getId());
    }

    /**
     * Retention ({@code WORKFLOW_STALE}): open workflows whose {@code opened_at} is before cutoff.
     */
    @Transactional(readOnly = true)
    public List<Long> findStaleWorkflowIds(Instant cutoff) {
        return workflowRepository.findStaleOpenWorkflowIds(cutoff);
    }

    /**
     * Retention ({@code WORKFLOW_STALE}): cancels one stale workflow in its own transaction, so a
     * failing handler callback does not roll back the other cancellations. Uses {@code FOR UPDATE
     * SKIP LOCKED} so parallel runs do not double-cancel.
     *
     * @return {@code false} if the workflow is locked by another run or no longer open
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancelStaleWorkflow(Long workflowId, Instant cutoff) {
        Optional<Workflow> locked =
                workflowRepository.findAndLockStaleOpenWorkflow(workflowId, cutoff);
        if (locked.isEmpty()) {
            return false;
        }
        Workflow workflow = locked.get();
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        applyCancellation(workflow, Instant.now(), true, handler, null);
        log.info(
                "Cancelled stale workflow {} opened at {}",
                workflow.getId(),
                workflow.getOpenedAt());
        return true;
    }

    private void applyCancellation(
            Workflow workflow,
            Instant at,
            boolean invokeHandlerCallback,
            WorkflowHandler<?> handler,
            Long actorUserId) {
        workflow.setStatus(WorkflowStatus.CANCELLED);
        workflow.setDecidedAt(at);
        cancelOpenInputChanges(workflow, at);
        workflowRepository.save(workflow);
        openExclusivityService.releaseLocks(workflow.getId());
        if (invokeHandlerCallback) {
            handler.onCancelled(workflow);
        }
        eventPublisher.publish(
                workflow,
                WorkflowLifecycleEvent.CANCELLED,
                WorkflowEventContext.ofActor(null, actorUserId));
    }

    @Transactional
    public WorkflowDto requestInputChange(
            Long workflowId, RequestWorkflowInputChangeRequest request) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireOpenWorkflow(workflowId);
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        if (!handler.allowsInputChangeRequests()) {
            throw new WorkflowAccessDeniedException(
                    "Input change requests are not allowed for this workflow type");
        }
        if (!WorkflowInputChangeRules.canRequestInputChange(workflow, userId)) {
            throw new WorkflowAccessDeniedException(
                    "Not allowed to request an input change for this workflow");
        }
        assertMayRequestInputChange(workflow, userId);

        applyInputChangeRequest(workflow, userId, request.getMessage(), handler);

        log.info("Requested input change on workflow {}", workflowId);
        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED,
                WorkflowEventContext.ofActor(null, userId));
        eventPublisher.publish(updated, WorkflowLifecycleEvent.UPDATED);
        return toEnrichedDto(updated);
    }

    private void assertMayRequestInputChange(Workflow workflow, Long userId) {
        if (selfApproveService.blocksInputChangeRequest(workflow, userId)) {
            throw new WorkflowAccessDeniedException(
                    "Self-approve is not allowed for this workflow");
        }
    }

    private void applyInputChangeRequest(
            Workflow workflow, Long userId, String message, WorkflowHandler<?> handler) {
        if (workflow.findOpenInputChange().isPresent()) {
            throw new WorkflowStateException(
                    "Workflow " + workflow.getId() + " already has an open input change request");
        }
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw new WorkflowValidationException("Input change message is required");
        }
        Instant now = Instant.now();
        WorkflowInputChange inputChange =
                WorkflowInputChange.builder()
                        .tenantId(workflow.getTenantId())
                        .workflow(workflow)
                        .requestedByUserId(userId)
                        .requestedAt(now)
                        .message(trimmed)
                        .status(WorkflowInputChangeStatus.OPEN)
                        .build();
        workflow.getInputChanges().add(inputChange);
        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        workflowRepository.save(workflow);
        handler.onInputChangeRequested(workflow);
    }

    @Transactional
    public WorkflowDto withdrawInputChange(Long workflowId) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireWorkflowWithDetails(workflowId);
        if (!WorkflowInputChangeRules.canWithdrawInputChange(workflow, userId)) {
            throw new WorkflowAccessDeniedException(
                    "Not allowed to withdraw the input change request for this workflow");
        }

        Instant now = Instant.now();
        WorkflowInputChange openChange =
                workflow.findOpenInputChange()
                        .orElseThrow(
                                () ->
                                        new WorkflowStateException(
                                                "Workflow "
                                                        + workflowId
                                                        + " has no open input change request"));
        openChange.cancel(now);
        workflow.setStatus(WorkflowStatus.OPEN);
        workflowRepository.save(workflow);
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        handler.onInputChangeWithdrawn(workflow);

        log.info("Withdrew input change request on workflow {}", workflowId);
        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.INPUT_CHANGE_WITHDRAWN,
                WorkflowEventContext.ofActor(null, userId));
        eventPublisher.publish(updated, WorkflowLifecycleEvent.UPDATED);
        return toEnrichedDto(updated);
    }

    @Transactional
    public WorkflowDto updatePayload(Long workflowId, Object payload) {
        Long userId = tokenService.requireUserId();
        Workflow workflow = requireWorkflowWithDetails(workflowId);
        if (!canEditInput(workflow, userId)) {
            throw new WorkflowAccessDeniedException(
                    "Not allowed to update the payload for this workflow");
        }

        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        Object typedPayload = convertPayload(handler, payload);
        WorkflowOpenContext<?> updateContext =
                invokeHandlerValidateUpdate(
                        handler, userId, workflow.getTenantId(), typedPayload, workflow);
        List<String> concurrencyKeys = resolveConcurrencyKeys(handler, updateContext, workflow);
        openConcurrencyService.assertNoConflict(handler, userId, concurrencyKeys, workflow.getId());
        List<String> exclusivityKeys = resolveExclusivityKeys(handler, updateContext, workflow);
        openExclusivityService.assertNoConflict(handler, exclusivityKeys, workflow.getId());

        boolean fulfillingChangeRequest =
                workflow.getStatus() == WorkflowStatus.AWAITING_INPUT_CHANGE;
        boolean hadHumanDecisions =
                workflow.getDecisions().stream().anyMatch(d -> d.getUserId() != null);

        workflow.setPayload(serializePayload(typedPayload));
        workflow.setOpenConcurrencyKeys(openConcurrencyService.serializeKeys(concurrencyKeys));
        if (fulfillingChangeRequest) {
            Instant now = Instant.now();
            WorkflowInputChange openChange =
                    workflow.findOpenInputChange()
                            .orElseThrow(
                                    () ->
                                            new WorkflowStateException(
                                                    "Workflow "
                                                            + workflowId
                                                            + " is awaiting input change but has no open request"));
            openChange.fulfill(now);
            workflow.setStatus(WorkflowStatus.OPEN);
        }

        if (hadHumanDecisions || hasNonPendingApproverSlots(workflow)) {
            resetDecisionsKeepingClaims(workflow);
            workflowRepository.save(workflow);
            Workflow afterReset = requireWorkflowWithDetails(workflowId);
            eventPublisher.publish(afterReset, WorkflowLifecycleEvent.DECISIONS_RESET);
        } else {
            workflowRepository.save(workflow);
        }

        openExclusivityService.replaceLocks(
                handler, workflow.getTenantId(), workflowId, exclusivityKeys);

        invokeHandlerInputUpdated(handler, requireWorkflowWithDetails(workflowId));

        log.info("Updated payload on workflow {}", workflowId);
        Workflow updated = requireWorkflowWithDetails(workflowId);
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.INPUT_UPDATED,
                WorkflowEventContext.ofActor(null, userId));
        eventPublisher.publish(updated, WorkflowLifecycleEvent.UPDATED);
        return toEnrichedDto(updated);
    }

    /**
     * Aligns open workflow instances with current mapping, definition, and static approver settings
     * after admin configuration changes. Side effects (todos, notifications on completion) use the
     * same lifecycle events as {@link #openForUser}.
     */
    @Transactional
    public void reconcileOpenInstancesAfterConfigurationChange(
            WorkflowConfigurationChangedEvent event) {
        if (event == null
                || event.tenantId() == null
                || event.workflowTypeId() == null
                || event.scope() == null) {
            return;
        }
        Long workflowTypeId = event.workflowTypeId();
        boolean tenantWide = event.scope() == WorkflowConfigurationScope.TENANT_WIDE;
        Set<Long> affectedOrgIds = event.organizationIds();
        WorkflowHandler<?> handler = handlerRegistry.require(workflowTypeId);
        List<Workflow> openWorkflows =
                workflowRepository.findOpenWithDetailsByWorkflowTypeId(workflowTypeId);
        for (Workflow workflow : openWorkflows) {
            OpenResolution resolution;
            try {
                resolution = resolveOpenContext(handler, workflow);
            } catch (Exception ex) {
                log.warn(
                        "Skipping reconcile for workflow {}: {}",
                        workflow.getId(),
                        ex.getMessage());
                continue;
            }
            WorkflowDefinition definition =
                    definitionService.resolveEffectiveDefinition(
                            workflowTypeId, resolution.contextOrgId());
            List<Long> staticOrgIds =
                    definition.getStaticOrganizations().stream()
                            .map(WorkflowDefinitionStaticOrg::getApproverOrganizationId)
                            .toList();

            WorkflowResolverService.OrgApproverSlotSyncResult slotSync =
                    resolverService.syncOrgApproverSlots(
                            workflow,
                            workflowTypeId,
                            resolution.contextOrgId(),
                            staticOrgIds,
                            definition.isStaticApproversExclusive(),
                            resolution.dynamicOrgIds());
            for (WorkflowParticipant removed : slotSync.removedPendingParticipants()) {
                if (removed.getId() == null) {
                    continue;
                }
                eventPublisher.publish(
                        workflow,
                        WorkflowLifecycleEvent.APPROVER_SLOT_REMOVED,
                        WorkflowEventContext.ofActor(removed.getId(), null));
            }

            boolean changed = slotSync.changed();
            boolean refreshApproverTodos = tenantWide;
            for (WorkflowParticipant participant : new ArrayList<>(workflow.getParticipants())) {
                if (!participant.isApprover()
                        || participant.getSlotStatus() != ParticipantSlotStatus.PENDING) {
                    continue;
                }
                if (participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP) {
                    if (!tenantWide
                            && (participant.getOrganizationId() == null
                                    || !affectedOrgIds.contains(participant.getOrganizationId()))) {
                        continue;
                    }
                    refreshApproverTodos = true;
                    Long orgId = participant.getOrganizationId();
                    WorkflowTypeMapping mapping =
                            mappingService.requireMapping(orgId, workflowTypeId);
                    WorkflowDefinition orgDefinition =
                            definitionService.resolveEffectiveDefinition(workflowTypeId, orgId);
                    ReconcileParticipantResult result =
                            reconcileApproverParticipant(
                                    workflow, participant, mapping, orgDefinition);
                    changed = changed || result.hasChanges();
                } else if (participant.getPrincipalType() == PrincipalType.USER) {
                    refreshApproverTodos = true;
                    WorkflowDefinition userDefinition =
                            definitionService.resolveEffectiveDefinition(
                                    workflowTypeId, resolution.contextOrgId());
                    ReconcileParticipantResult result =
                            reconcileUserApproverParticipant(workflow, participant, userDefinition);
                    changed = changed || result.hasChanges();
                }
            }

            if (!changed && !refreshApproverTodos) {
                continue;
            }

            WorkflowStatus statusBefore = workflow.getStatus();
            if (changed) {
                workflowRepository.save(workflow);
                recordAutoApproveDecisions(workflow);
                Workflow refreshed = requireWorkflowWithDetails(workflow.getId());
                completeIfAllApproved(refreshed, handler);
            }
            Workflow outcome = changed ? requireWorkflowWithDetails(workflow.getId()) : workflow;
            publishReconcileOutcome(outcome, statusBefore);
            log.debug(
                    "Reconciled open workflow {} after configuration change for type id {}",
                    workflow.getId(),
                    workflowTypeId);
        }
    }

    private void publishReconcileOutcome(Workflow workflow, WorkflowStatus statusBefore) {
        if (workflow.getStatus() == WorkflowStatus.APPROVED
                && statusBefore == WorkflowStatus.OPEN) {
            eventPublisher.publish(workflow, WorkflowLifecycleEvent.UPDATED);
            return;
        }
        if (hasPendingApproverSlots(workflow)) {
            eventPublisher.publish(workflow, WorkflowLifecycleEvent.OPENED);
        }
        eventPublisher.publish(workflow, WorkflowLifecycleEvent.UPDATED);
    }

    private WorkflowDto toEnrichedDto(Workflow workflow) {
        WorkflowDto dto = mapper.toDto(workflow);
        dto.setOpenConcurrencyKeys(
                openConcurrencyService.deserializeKeys(workflow.getOpenConcurrencyKeys()));
        enrichSelfApprove(dto, workflow);
        Long currentUserId = tokenService.getUserId();
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        dto.setCanEditInput(canEditInput(workflow, currentUserId));
        dto.setCanRequestInputChange(canRequestInputChange(workflow, currentUserId));
        dto.setCanWithdrawInputChange(
                WorkflowInputChangeRules.canWithdrawInputChange(workflow, currentUserId));
        dto.setMapsRejectToInputChange(handler.mapsRejectToInputChange());
        dto.setActiveInputChange(workflow.findOpenInputChange().map(mapper::toDto).orElse(null));
        enrichOpenerDisplayNames(List.of(dto));
        enrichParticipants(dto.getParticipants(), workflow.getTenantId());
        enrichDecisions(dto.getDecisions());
        return dto;
    }

    private List<WorkflowDto> toEnrichedDtoList(List<Workflow> workflows) {
        List<WorkflowDto> dtos = mapper.toDtoList(workflows);
        Long currentUserId = tokenService.getUserId();
        enrichOpenerDisplayNames(dtos);
        for (int i = 0; i < workflows.size(); i++) {
            Workflow workflow = workflows.get(i);
            WorkflowDto dto = dtos.get(i);
            WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
            dto.setOpenConcurrencyKeys(
                    openConcurrencyService.deserializeKeys(workflow.getOpenConcurrencyKeys()));
            enrichSelfApprove(dto, workflow);
            dto.setCanEditInput(canEditInput(workflow, currentUserId));
            dto.setCanRequestInputChange(canRequestInputChange(workflow, currentUserId));
            dto.setCanWithdrawInputChange(
                    WorkflowInputChangeRules.canWithdrawInputChange(workflow, currentUserId));
            dto.setMapsRejectToInputChange(handler.mapsRejectToInputChange());
            dto.setActiveInputChange(
                    workflow.findOpenInputChange().map(mapper::toDto).orElse(null));
            enrichParticipants(dto.getParticipants(), workflow.getTenantId());
            enrichDecisions(dto.getDecisions());
        }
        return dtos;
    }

    private void enrichOpenerDisplayNames(List<WorkflowDto> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        Set<Long> openerIds =
                dtos.stream()
                        .map(WorkflowDto::getOpenedByUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (openerIds.isEmpty()) {
            return;
        }
        Map<Long, User> usersById =
                userRepository.findAllById(openerIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        for (WorkflowDto dto : dtos) {
            User opener = usersById.get(dto.getOpenedByUserId());
            if (opener != null) {
                dto.setOpenedByDisplayName(formatUserDisplayName(opener));
            }
        }
    }

    private void enrichSelfApprove(WorkflowDto dto, Workflow workflow) {
        if (dto.getParticipants() == null) {
            return;
        }
        for (WorkflowParticipantDto participant : dto.getParticipants()) {
            participant.setSelfApproveAllowed(
                    selfApproveService.isAllowed(workflow, participant.getOrganizationId()));
        }
    }

    private void enrichParticipants(List<WorkflowParticipantDto> participants, Long tenantId) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        Set<Long> userIds =
                participants.stream()
                        .flatMap(
                                p -> {
                                    List<Long> ids = new ArrayList<>();
                                    if (p.getPrincipalType() == PrincipalType.USER
                                            && p.getPrincipalId() != null) {
                                        ids.add(p.getPrincipalId());
                                    }
                                    if (p.getClaimedByUserId() != null) {
                                        ids.add(p.getClaimedByUserId());
                                    }
                                    return ids.stream();
                                })
                        .collect(Collectors.toSet());
        Map<Long, User> usersById =
                userIds.isEmpty()
                        ? Map.of()
                        : userRepository.findAllById(userIds).stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));

        Set<Long> groupIds =
                participants.stream()
                        .filter(
                                p ->
                                        p.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                                                && p.getPrincipalId() != null)
                        .map(WorkflowParticipantDto::getPrincipalId)
                        .collect(Collectors.toSet());
        Map<Long, OrganizationGroup> groupsById =
                groupIds.isEmpty()
                        ? Map.of()
                        : organizationGroupRepository.findAllById(groupIds).stream()
                                .collect(
                                        Collectors.toMap(
                                                OrganizationGroup::getId, Function.identity()));

        Set<Long> organizationIds =
                participants.stream()
                        .map(WorkflowParticipantDto::getOrganizationId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Long, Organization> organizationsById =
                organizationIds.isEmpty()
                        ? Map.of()
                        : organizationRepository.findAllById(organizationIds).stream()
                                .collect(
                                        Collectors.toMap(Organization::getId, Function.identity()));

        for (WorkflowParticipantDto participant : participants) {
            if (participant.getOrganizationId() != null) {
                Organization organization = organizationsById.get(participant.getOrganizationId());
                if (organization != null) {
                    participant.setOrganizationDisplayName(organization.getName());
                }
            }
            if (participant.getPrincipalType() == PrincipalType.USER
                    && participant.getPrincipalId() != null) {
                User user = usersById.get(participant.getPrincipalId());
                if (user != null) {
                    participant.setPrincipalDisplayName(formatUserDisplayName(user));
                }
            } else if (participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                    && participant.getPrincipalId() != null) {
                OrganizationGroup group = groupsById.get(participant.getPrincipalId());
                if (group != null) {
                    participant.setPrincipalDisplayName(group.getName());
                }
            }
            if (participant.getClaimedByUserId() != null) {
                User claimer = usersById.get(participant.getClaimedByUserId());
                if (claimer != null) {
                    participant.setClaimedByDisplayName(formatUserDisplayName(claimer));
                }
            }
        }
    }

    private void enrichDecisions(List<WorkflowDecisionDto> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return;
        }
        Set<Long> userIds =
                decisions.stream()
                        .map(WorkflowDecisionDto::getUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> usersById =
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        for (WorkflowDecisionDto decision : decisions) {
            if (decision.getUserId() == null) {
                continue;
            }
            User user = usersById.get(decision.getUserId());
            if (user != null) {
                decision.setUserDisplayName(formatUserDisplayName(user));
            }
        }
    }

    private ReconcileParticipantResult reconcileUserApproverParticipant(
            Workflow workflow, WorkflowParticipant participant, WorkflowDefinition definition) {
        if (participant.getSlotStatus() != ParticipantSlotStatus.PENDING
                || participant.getPrincipalType() != PrincipalType.USER) {
            return ReconcileParticipantResult.unchanged();
        }
        Long openerId = workflow.getOpenedByUserId();
        Long principalId = participant.getPrincipalId();
        if (openerId == null || principalId == null || !principalId.equals(openerId)) {
            return ReconcileParticipantResult.unchanged();
        }
        boolean changed = false;
        if (!definition.isSelfApproveAllowed()) {
            if (participant.getClaimedByUserId() != null) {
                participantClaimService.releasePendingClaim(
                        participant, participant.getClaimedByUserId());
                changed = true;
            }
        } else if (participant.getClaimedByUserId() == null
                && WorkflowResolverService.mayAutoClaimUserApprover(openerId, true, principalId)) {
            participant.setClaimedByUserId(principalId);
            participant.setClaimedAt(Instant.now());
            changed = true;
        }
        return changed
                ? ReconcileParticipantResult.updated()
                : ReconcileParticipantResult.unchanged();
    }

    private ReconcileParticipantResult reconcileApproverParticipant(
            Workflow workflow,
            WorkflowParticipant participant,
            WorkflowTypeMapping mapping,
            WorkflowDefinition definition) {
        boolean changed = false;

        Long newGroupId = mapping.getOrganizationGroupId();
        if (newGroupId != null && !newGroupId.equals(participant.getPrincipalId())) {
            if (participant.getSlotStatus() == ParticipantSlotStatus.PENDING
                    && participant.getClaimedByUserId() != null) {
                participantClaimService.releasePendingClaim(
                        participant, participant.getClaimedByUserId());
            }
            participant.setPrincipalId(newGroupId);
            changed = true;
        }

        if (mapping.isAutoApprove()
                && participant.getSlotStatus() == ParticipantSlotStatus.PENDING) {
            participant.setClaimedByUserId(null);
            participant.setClaimedAt(null);
            participant.setSlotStatus(ParticipantSlotStatus.APPROVED);
            ensureAutoApprovalDecision(workflow, participant);
            changed = true;
        }

        if (participant.getSlotStatus() == ParticipantSlotStatus.PENDING
                && participant.getClaimedByUserId() != null
                && participant.getClaimedByUserId().equals(workflow.getOpenedByUserId())
                && !definition.isSelfApproveAllowed()) {
            participantClaimService.releasePendingClaim(
                    participant, participant.getClaimedByUserId());
            changed = true;
        }

        return changed
                ? ReconcileParticipantResult.updated()
                : ReconcileParticipantResult.unchanged();
    }

    private void ensureAutoApprovalDecision(Workflow workflow, WorkflowParticipant participant) {
        boolean hasDecision =
                workflow.getDecisions().stream()
                        .anyMatch(
                                decision ->
                                        participant.getId().equals(decision.getParticipantId())
                                                && decision.getDecisionType()
                                                        == DecisionType.APPROVED);
        if (!hasDecision) {
            workflow.getDecisions()
                    .add(
                            WorkflowDecision.builder()
                                    .tenantId(workflow.getTenantId())
                                    .workflow(workflow)
                                    .participantId(participant.getId())
                                    .userId(null)
                                    .decisionType(DecisionType.APPROVED)
                                    .decidedAt(Instant.now())
                                    .build());
        }
    }

    private void recordAutoApproveDecisions(Workflow workflow) {
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (participant.isApprover()
                    && participant.getSlotStatus() == ParticipantSlotStatus.APPROVED) {
                ensureAutoApprovalDecision(workflow, participant);
            }
        }
        if (!workflow.getDecisions().isEmpty()) {
            workflowRepository.save(workflow);
        }
    }

    private void completeIfAllApproved(Workflow workflow, WorkflowHandler<?> handler) {
        if (workflow.getStatus() != WorkflowStatus.OPEN) {
            return;
        }
        List<WorkflowParticipant> approverSlots =
                workflow.getParticipants().stream()
                        .filter(WorkflowParticipant::isApprover)
                        .toList();
        if (approverSlots.isEmpty()) {
            return;
        }
        boolean allApproved =
                approverSlots.stream()
                        .allMatch(p -> p.getSlotStatus() == ParticipantSlotStatus.APPROVED);
        if (allApproved) {
            workflow.setStatus(WorkflowStatus.APPROVED);
            workflow.setDecidedAt(Instant.now());
            workflowRepository.save(workflow);
            openExclusivityService.releaseLocks(workflow.getId());
            handler.onApproved(workflow);
            List<Long> promotedDocumentIds =
                    documentPromotionService.promoteToUserDocuments(
                            workflow,
                            documentPromotionService.resolvePromotionsForCompletion(
                                    workflow, handler),
                            handler);
            handler.onDocumentsPromoted(workflow, promotedDocumentIds);
            eventPublisher.publish(workflow, WorkflowLifecycleEvent.APPROVED);
            log.info("Workflow {} approved (all slots resolved)", workflow.getId());
        }
    }

    private Workflow requireOpenWorkflow(Long workflowId) {
        Workflow workflow = requireWorkflowWithDetails(workflowId);
        if (workflow.getStatus() != WorkflowStatus.OPEN) {
            throw new WorkflowStateException("Workflow " + workflowId + " is not open");
        }
        return workflow;
    }

    private Workflow requireActiveWorkflow(Long workflowId) {
        Workflow workflow = requireWorkflowWithDetails(workflowId);
        if (!workflow.getStatus().isActive()) {
            throw new WorkflowStateException("Workflow " + workflowId + " is not active");
        }
        return workflow;
    }

    private boolean canEditInput(Workflow workflow, Long currentUserId) {
        if (!WorkflowInputChangeRules.canEditInput(workflow, currentUserId)) {
            return false;
        }
        if (workflow.getStatus() == WorkflowStatus.AWAITING_INPUT_CHANGE) {
            return true;
        }
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        return handler.allowsOpenerFreeInputEdit();
    }

    private boolean canRequestInputChange(Workflow workflow, Long currentUserId) {
        WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
        if (!handler.allowsInputChangeRequests()) {
            return false;
        }
        if (!WorkflowInputChangeRules.canRequestInputChange(workflow, currentUserId)) {
            return false;
        }
        return !selfApproveService.blocksInputChangeRequest(workflow, currentUserId);
    }

    private void cancelOpenInputChanges(Workflow workflow, Instant at) {
        for (WorkflowInputChange change : workflow.getInputChanges()) {
            if (change.isOpen()) {
                change.cancel(at);
            }
        }
    }

    private boolean hasNonPendingApproverSlots(Workflow workflow) {
        return workflow.getParticipants().stream()
                .anyMatch(
                        p -> p.isApprover() && p.getSlotStatus() != ParticipantSlotStatus.PENDING);
    }

    private void resetDecisionsKeepingClaims(Workflow workflow) {
        workflow.getDecisions().removeIf(d -> d.getUserId() != null);
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (!participant.isApprover()) {
                continue;
            }
            if (participant.getSlotStatus() == ParticipantSlotStatus.APPROVED
                    || participant.getSlotStatus() == ParticipantSlotStatus.REJECTED) {
                // Keep system auto-approvals (decision with null userId) as APPROVED.
                boolean hasSystemAutoApprove =
                        workflow.getDecisions().stream()
                                .anyMatch(
                                        d ->
                                                participant.getId().equals(d.getParticipantId())
                                                        && d.getDecisionType()
                                                                == DecisionType.APPROVED
                                                        && d.getUserId() == null);
                if (!hasSystemAutoApprove) {
                    participant.setSlotStatus(ParticipantSlotStatus.PENDING);
                }
            }
        }
    }

    private Object convertPayload(WorkflowHandler<?> handlerRaw, Object payload) {
        if (payload == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        Object typedPayload;
        if (handler.payloadType().isInstance(payload)) {
            typedPayload = payload;
        } else {
            try {
                typedPayload = jsonMapper.convertValue(payload, handler.payloadType());
            } catch (IllegalArgumentException e) {
                throw new WorkflowValidationException(
                        "Invalid workflow payload: " + e.getMessage());
            }
        }
        requireValidPayload(typedPayload);
        return typedPayload;
    }

    /** Same bean-validation constraints as {@code @Valid} on the type-specific open endpoints. */
    private void requireValidPayload(Object typedPayload) {
        if (typedPayload == null) {
            return;
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(typedPayload);
        if (!violations.isEmpty()) {
            String details =
                    violations.stream()
                            .map(v -> v.getPropertyPath() + " " + v.getMessage())
                            .sorted()
                            .collect(Collectors.joining("; "));
            throw new WorkflowValidationException("Invalid workflow payload: " + details);
        }
    }

    @SuppressWarnings("unchecked")
    private WorkflowOpenContext<?> invokeHandlerValidateUpdate(
            WorkflowHandler<?> handlerRaw,
            Long openerId,
            Long tenantId,
            Object payload,
            Workflow existing) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        Object typedPayload = payload == null ? null : handler.payloadType().cast(payload);
        WorkflowOpenContext<Object> ctx =
                new WorkflowOpenContext<>(openerId, tenantId, typedPayload);
        handler.validateUpdate(ctx, existing);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private void invokeHandlerInputUpdated(WorkflowHandler<?> handlerRaw, Workflow workflow) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        handler.onInputUpdated(workflow);
    }

    private Workflow requireWorkflowWithDetails(Long workflowId) {
        return workflowRepository
                .findWithDetailsById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    private WorkflowParticipant requireParticipant(Long workflowId, Long participantId) {
        return participantRepository
                .findByIdAndWorkflow_Id(participantId, workflowId)
                .orElseThrow(() -> new WorkflowParticipantNotFoundException(participantId));
    }

    private boolean isActiveGroupMember(Long userId, Long organizationId, Long groupId) {
        if (groupId == null) {
            return false;
        }
        if (organizationId != null) {
            return membershipRepository
                    .findActiveGroupIdsForUserInOrganization(userId, organizationId, Instant.now())
                    .contains(groupId);
        }
        return activeGroupIds(userId).contains(groupId);
    }

    private List<Long> activeGroupIds(Long userId) {
        Instant now = Instant.now();
        List<Long> groupIds = new ArrayList<>();
        for (UserOrganizationGroupMembership membership :
                membershipRepository.findByUser_Id(userId)) {
            if (membership.isActiveAt(now)) {
                groupIds.add(membership.getOrganizationGroup().getId());
            }
        }
        if (groupIds.isEmpty()) {
            groupIds.add(-1L);
        }
        return groupIds;
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new WorkflowValidationException(
                    "Unable to serialize workflow payload: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private OpenResolution resolveOpenContext(WorkflowHandler<?> handlerRaw, Workflow workflow) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        Object payload = deserializePayload(handler, workflow);
        WorkflowOpenContext<Object> ctx =
                new WorkflowOpenContext<>(
                        workflow.getOpenedByUserId(), workflow.getTenantId(), payload);

        Long contextOrgId = handler.resolveContextOrganization(ctx);
        if (contextOrgId == null) {
            throw new WorkflowValidationException(
                    "Handler did not resolve a context organization for type id "
                            + handler.workflowTypeId());
        }
        List<Long> dynamicOrgIds =
                handler.mode() == WorkflowMode.DYNAMIC
                        ? handler.resolveDynamicOrganizations(ctx).stream()
                                .map(ApproverOrg::organizationId)
                                .toList()
                        : List.of();
        List<ParticipantSpec> additional = handler.additionalParticipants(ctx);
        return new OpenResolution(contextOrgId, dynamicOrgIds, additional, List.of(), List.of());
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveConcurrencyKeys(
            WorkflowHandler<?> handlerRaw, WorkflowOpenContext<?> ctx, Workflow existing) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        WorkflowOpenContext<Object> typedCtx =
                new WorkflowOpenContext<>(
                        ctx.getOpenedByUserId(), ctx.getTenantId(), ctx.getPayload());
        return handler.resolveOpenConcurrencyKeys(typedCtx, existing);
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveExclusivityKeys(
            WorkflowHandler<?> handlerRaw, WorkflowOpenContext<?> ctx, Workflow existing) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        WorkflowOpenContext<Object> typedCtx =
                new WorkflowOpenContext<>(
                        ctx.getOpenedByUserId(), ctx.getTenantId(), ctx.getPayload());
        return handler.resolveOpenExclusivityKeys(typedCtx, existing);
    }

    @SuppressWarnings("unchecked")
    private Object deserializePayload(WorkflowHandler<?> handlerRaw, Workflow workflow) {
        if (workflow.getPayload() == null) {
            return null;
        }
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        try {
            return jsonMapper.readValue(workflow.getPayload(), handler.payloadType());
        } catch (JacksonException e) {
            throw new WorkflowValidationException(
                    "Unable to deserialize workflow payload: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeHandlerOpened(WorkflowHandler<?> handlerRaw, Workflow workflow) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        handler.onOpened(workflow);
    }

    @SuppressWarnings("unchecked")
    private OpenResolution invokeHandlerOpen(
            WorkflowHandler<?> handlerRaw, Long openerId, Long tenantId, Object payload) {
        WorkflowHandler<Object> handler = (WorkflowHandler<Object>) handlerRaw;
        Object typedPayload = payload == null ? null : handler.payloadType().cast(payload);
        WorkflowOpenContext<Object> ctx =
                new WorkflowOpenContext<>(openerId, tenantId, typedPayload);

        handler.validateOpen(ctx);
        Long contextOrgId = handler.resolveContextOrganization(ctx);
        if (contextOrgId == null) {
            throw new WorkflowValidationException(
                    "Handler did not resolve a context organization for type id "
                            + handler.workflowTypeId());
        }
        Long permissionCheckOrgId = handler.resolvePermissionCheckOrganization(ctx, contextOrgId);
        if (permissionCheckOrgId == null) {
            throw new WorkflowValidationException(
                    "Handler did not resolve a permission-check organization for type id "
                            + handler.workflowTypeId());
        }
        handler.requiredPermissionToOpen()
                .ifPresent(
                        permissionType -> {
                            if (!organizationAccessService.hasOrganizationPermission(
                                    permissionCheckOrgId, permissionType)) {
                                throw new WorkflowAccessDeniedException(
                                        "Missing permission "
                                                + permissionType
                                                + " in organization "
                                                + permissionCheckOrgId
                                                + " to open workflow type "
                                                + handler.workflowTypeId());
                            }
                        });
        List<Long> dynamicOrgIds =
                handler.mode() == WorkflowMode.DYNAMIC
                        ? handler.resolveDynamicOrganizations(ctx).stream()
                                .map(ApproverOrg::organizationId)
                                .toList()
                        : List.of();
        List<ParticipantSpec> additional = handler.additionalParticipants(ctx);
        List<String> concurrencyKeys = handler.resolveOpenConcurrencyKeys(ctx, null);
        openConcurrencyService.assertNoConflict(handler, openerId, concurrencyKeys, null);
        List<String> exclusivityKeys = handler.resolveOpenExclusivityKeys(ctx, null);
        openExclusivityService.assertNoConflict(handler, exclusivityKeys, null);
        return new OpenResolution(
                contextOrgId, dynamicOrgIds, additional, concurrencyKeys, exclusivityKeys);
    }

    private WorkflowOpenStateDto buildOpenState(Long userId, long workflowTypeId) {
        WorkflowHandler<?> handler = handlerRegistry.require(workflowTypeId);
        boolean typeBlocked =
                !handler.allowsMultipleConcurrentOpen()
                        && workflowRepository.existsActiveByOpenedByUserIdAndWorkflowTypeId(
                                userId, workflowTypeId);
        return WorkflowOpenStateDto.builder()
                .workflowTypeId(workflowTypeId)
                .typeBlocked(typeBlocked)
                .openConcurrencyKeys(
                        List.copyOf(openConcurrencyService.findOpenKeys(userId, workflowTypeId)))
                .build();
    }

    private record ReconcileParticipantResult(boolean hasChanges) {

        static ReconcileParticipantResult unchanged() {
            return new ReconcileParticipantResult(false);
        }

        static ReconcileParticipantResult updated() {
            return new ReconcileParticipantResult(true);
        }
    }

    private record OpenResolution(
            Long contextOrgId,
            List<Long> dynamicOrgIds,
            List<ParticipantSpec> additional,
            List<String> openConcurrencyKeys,
            List<String> openExclusivityKeys) {}
}
