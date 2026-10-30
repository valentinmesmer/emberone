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
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.security.WorkflowInstanceAccess;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.dto.*;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowEventPublisher;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.exception.WorkflowAccessDeniedException;
import com.emberone.workflow.exception.WorkflowNotFoundException;
import com.emberone.workflow.exception.WorkflowStateException;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.list.WorkflowListFilter;
import com.emberone.workflow.list.WorkflowListSearch;
import com.emberone.workflow.mapper.WorkflowMapper;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import com.emberone.workflow.spi.WorkflowOpenContext;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock WorkflowDefinitionService definitionService;
    @Mock WorkflowTypeMappingService mappingService;
    @Mock WorkflowSelfApproveService selfApproveService;
    @Mock WorkflowResolverService resolverService;
    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowListSearch workflowListSearch;
    @Mock WorkflowParticipantRepository participantRepository;
    @Mock WorkflowMapper mapper;
    @Mock WorkflowEventPublisher eventPublisher;
    @Mock TokenService tokenService;
    @Mock UserOrganizationGroupMembershipRepository membershipRepository;
    @Mock UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserRepository userRepository;
    @Mock JsonMapper jsonMapper;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock WorkflowInstanceAccess workflowInstanceAccess;
    @Mock WorkflowDocumentPromotionService documentPromotionService;
    @Mock WorkflowParticipantClaimService participantClaimService;
    @Mock WorkflowOpenConcurrencyService openConcurrencyService;
    @Mock WorkflowOpenExclusivityService openExclusivityService;
    @Spy Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock WorkflowHandler<Object> handler;

    @InjectMocks WorkflowService service;

    @BeforeEach
    void setUpDtoMapping() {
        lenient()
                .when(workflowListSearch.search(any(ListQuery.class)))
                .thenReturn(Specification.unrestricted());
        lenient()
                .when(mapper.toDto(any(Workflow.class)))
                .thenReturn(
                        WorkflowDto.builder()
                                .participants(new ArrayList<>())
                                .decisions(new ArrayList<>())
                                .build());
        lenient().doReturn(handler).when(handlerRegistry).require(anyLong());
        lenient().when(handler.allowsInputChangeRequests()).thenReturn(true);
        lenient().when(handler.allowsOpenerFreeInputEdit()).thenReturn(true);
        lenient().when(handler.mapsRejectToInputChange()).thenReturn(false);
        lenient()
                .when(
                        userOrganizationMembershipRepository.existsActiveByUser_Id(
                                anyLong(), any(Instant.class)))
                .thenReturn(true);
    }

    @Test
    void approveDecisionCompletesWorkflowAndCallsCompletionHooks() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 42L);
        workflow.getParticipants().add(participant);
        arrangeOpenWorkflow(workflow, participant, 42L);

        service.decide(1L, decision(10L, DecisionType.APPROVED));

        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.APPROVED);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.APPROVED);
        assertThat(workflow.getDecisions())
                .singleElement()
                .satisfies(
                        recorded -> {
                            assertThat(recorded.getParticipantId()).isEqualTo(10L);
                            assertThat(recorded.getUserId()).isEqualTo(42L);
                            assertThat(recorded.getDecisionType()).isEqualTo(DecisionType.APPROVED);
                        });
        verify(handler).onApproved(workflow);
        verify(documentPromotionService).promoteToUserDocuments(any(), any(), any());
        verify(handler).onDocumentsPromoted(eq(workflow), any());
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.APPROVED);
        verify(eventPublisher)
                .publish(
                        workflow,
                        WorkflowLifecycleEvent.DECISION_RECORDED,
                        WorkflowEventContext.ofActor(10L, 42L));
    }

    @Test
    void rejectDecisionRejectsWholeWorkflow() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 42L);
        workflow.getParticipants().add(participant);
        arrangeOpenWorkflow(workflow, participant, 42L);

        service.decide(1L, decision(10L, DecisionType.REJECTED));

        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.REJECTED);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.REJECTED);
        assertThat(workflow.getDecidedAt()).isNotNull();
        verify(handler).onRejected(workflow);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.REJECTED);
        verify(handler, never()).onApproved(any());
        verify(eventPublisher)
                .publish(
                        workflow,
                        WorkflowLifecycleEvent.DECISION_RECORDED,
                        WorkflowEventContext.ofActor(10L, 42L));
    }

    @Test
    void rejectMapsToInputChangeWhenHandlerConfigured() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 7L);
        workflow.getParticipants().add(participant);
        arrangeOpenWorkflow(workflow, participant, 7L);
        when(handler.mapsRejectToInputChange()).thenReturn(true);

        service.decide(
                1L,
                WorkflowDecisionRequest.builder()
                        .participantId(10L)
                        .decision(DecisionType.REJECTED)
                        .comment("Bitte Führerschein nachreichen")
                        .build());

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.AWAITING_INPUT_CHANGE);
        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.PENDING);
        assertThat(workflow.getDecisions()).isEmpty();
        assertThat(workflow.findOpenInputChange())
                .isPresent()
                .get()
                .extracting(WorkflowInputChange::getMessage)
                .isEqualTo("Bitte Führerschein nachreichen");
        verify(handler).onInputChangeRequested(workflow);
        verify(handler, never()).onRejected(any());
        verify(eventPublisher, never()).publish(any(), eq(WorkflowLifecycleEvent.REJECTED));
        verify(eventPublisher)
                .publish(
                        any(Workflow.class),
                        eq(WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED),
                        any());
    }

    @Test
    void rejectMappedToInputChangeRequiresComment() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 7L);
        workflow.getParticipants().add(participant);
        arrangeOpenWorkflow(workflow, participant, 7L);
        when(handler.mapsRejectToInputChange()).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.decide(
                                        1L,
                                        WorkflowDecisionRequest.builder()
                                                .participantId(10L)
                                                .decision(DecisionType.REJECTED)
                                                .comment("   ")
                                                .build()))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("message");

        verify(workflowRepository, never()).save(any());
    }

    @Test
    void requestInputChangeDeniedWhenHandlerDisallows() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 7L);
        workflow.getParticipants().add(participant);
        arrangeWorkflow(workflow, 7L);
        when(handler.allowsInputChangeRequests()).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.requestInputChange(
                                        1L,
                                        RequestWorkflowInputChangeRequest.builder()
                                                .message("Bitte nachbessern")
                                                .build()))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("not allowed for this workflow type");

        verify(workflowRepository, never()).save(any());
    }

    @Test
    void decideRequiresClaimByCurrentUser() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 99L);
        arrangeOpenWorkflow(workflow, participant, 42L);

        assertThatThrownBy(() -> service.decide(1L, decision(10L, DecisionType.APPROVED)))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("claimer");

        verify(workflowRepository, never()).save(any());
    }

    @Test
    void activeGroupMemberCanClaimPendingOrganizationGroupSlot() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, null);
        arrangeWorkflowAndParticipant(workflow, participant, 42L);
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        eq(42L), eq(20L), any(Instant.class)))
                .thenReturn(List.of(30L));

        service.claim(1L, 10L);

        assertThat(participant.getClaimedByUserId()).isEqualTo(42L);
        assertThat(participant.getClaimedAt()).isNotNull();
        verify(participantRepository).save(participant);
    }

    @Test
    void nonMemberCannotClaimSlot() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, null);
        arrangeWorkflowAndParticipant(workflow, participant, 42L);
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        eq(42L), eq(20L), any(Instant.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.claim(1L, 10L))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("active member");

        verify(participantRepository, never()).save(any());
    }

    @Test
    void onlyClaimerCanUnclaimAndSuccessfulUnclaimClearsClaim() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 99L);
        arrangeWorkflowAndParticipant(workflow, participant, 42L);

        assertThatThrownBy(() -> service.unclaim(1L, 10L))
                .isInstanceOf(WorkflowAccessDeniedException.class);

        participant.setClaimedByUserId(42L);
        doAnswer(
                        invocation -> {
                            WorkflowParticipant p = invocation.getArgument(0);
                            p.setClaimedByUserId(null);
                            p.setClaimedAt(null);
                            return null;
                        })
                .when(participantClaimService)
                .releasePendingClaim(participant, 42L);
        service.unclaim(1L, 10L);
        assertThat(participant.getClaimedByUserId()).isNull();
        assertThat(participant.getClaimedAt()).isNull();
        verify(participantClaimService).releasePendingClaim(participant, 42L);
    }

    @Test
    void openerCanCancelOpenWorkflow() {
        Workflow workflow = workflow(1L);
        arrangeWorkflow(workflow, 42L);

        service.cancel(1L);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(workflow.getDecidedAt()).isNotNull();
        verify(handler).onCancelled(workflow);
        verify(eventPublisher)
                .publish(
                        workflow,
                        WorkflowLifecycleEvent.CANCELLED,
                        WorkflowEventContext.ofActor(null, 42L));
    }

    @Test
    void cancelActiveWorkflowsOpenedByUserCancelsWithoutHandlerCallback() {
        Workflow workflow = workflow(1L);
        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        when(workflowRepository.findActiveByOpenedByUserId(42L)).thenReturn(List.of(workflow));

        service.cancelActiveWorkflowsOpenedByUser(42L);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(workflow.getDecidedAt()).isNotNull();
        verify(handler, never()).onCancelled(any());
        verify(eventPublisher)
                .publish(
                        workflow,
                        WorkflowLifecycleEvent.CANCELLED,
                        WorkflowEventContext.ofActor(null, null));
        verify(workflowRepository).save(workflow);
    }

    @Test
    void claimerCanRequestInputChangeAndCallsHandler() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 7L);
        workflow.getParticipants().add(participant);
        arrangeWorkflow(workflow, 7L);

        service.requestInputChange(
                1L,
                RequestWorkflowInputChangeRequest.builder().message("Bitte nachbessern").build());

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.AWAITING_INPUT_CHANGE);
        assertThat(workflow.findOpenInputChange()).isPresent();
        verify(handler).onInputChangeRequested(workflow);
        verify(eventPublisher)
                .publish(
                        any(Workflow.class),
                        eq(WorkflowLifecycleEvent.INPUT_CHANGE_REQUESTED),
                        any());
        verify(eventPublisher).publish(any(Workflow.class), eq(WorkflowLifecycleEvent.UPDATED));
    }

    @Test
    void nonOpenerCannotCancelWorkflow() {
        Workflow workflow = workflow(1L);
        arrangeWorkflow(workflow, 99L);

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("opener");

        verify(handlerRegistry, never()).require(any());
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void requesterCanWithdrawOpenInputChange() {
        Workflow workflow = workflow(1L);
        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        workflow.setInputChanges(new LinkedHashSet<>());
        WorkflowInputChange openChange =
                WorkflowInputChange.builder()
                        .tenantId(7L)
                        .workflow(workflow)
                        .requestedByUserId(7L)
                        .message("Bitte Datum korrigieren")
                        .status(WorkflowInputChangeStatus.OPEN)
                        .build();
        workflow.getInputChanges().add(openChange);
        arrangeWorkflow(workflow, 7L);

        service.withdrawInputChange(1L);

        assertThat(openChange.getStatus()).isEqualTo(WorkflowInputChangeStatus.CANCELLED);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.OPEN);
        verify(workflowRepository).save(workflow);
        verify(handler).onInputChangeWithdrawn(workflow);
        verify(eventPublisher)
                .publish(
                        any(Workflow.class),
                        eq(WorkflowLifecycleEvent.INPUT_CHANGE_WITHDRAWN),
                        any());
        verify(eventPublisher).publish(any(Workflow.class), eq(WorkflowLifecycleEvent.UPDATED));
    }

    @Test
    void nonRequesterCannotWithdrawInputChange() {
        Workflow workflow = workflow(1L);
        workflow.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        workflow.setInputChanges(new LinkedHashSet<>());
        workflow.getInputChanges()
                .add(
                        WorkflowInputChange.builder()
                                .tenantId(7L)
                                .workflow(workflow)
                                .requestedByUserId(7L)
                                .message("Bitte Datum korrigieren")
                                .status(WorkflowInputChangeStatus.OPEN)
                                .build());
        arrangeWorkflow(workflow, 99L);

        assertThatThrownBy(() -> service.withdrawInputChange(1L))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("withdraw");

        verify(workflowRepository, never()).save(any());
    }

    @Test
    void withdrawApprovalRestoresPendingSlotAndRemovesOwnDecision() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 2L, 7L);
        participant.setSlotStatus(ParticipantSlotStatus.APPROVED);
        WorkflowDecision decision =
                WorkflowDecision.builder()
                        .workflow(workflow)
                        .participantId(2L)
                        .userId(7L)
                        .decisionType(DecisionType.APPROVED)
                        .build();
        workflow.getDecisions().add(decision);
        arrangeOpenWorkflow(workflow, participant, 7L);

        service.withdrawApproval(1L, 2L);

        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.PENDING);
        assertThat(workflow.getDecisions()).doesNotContain(decision);
        verify(participantRepository).save(participant);
        verify(workflowRepository).save(workflow);
    }

    @Test
    void withdrawApprovalRequiresOriginalApproverAndCurrentClaim() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 2L, 8L);
        participant.setSlotStatus(ParticipantSlotStatus.APPROVED);
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .workflow(workflow)
                                .participantId(2L)
                                .userId(7L)
                                .decisionType(DecisionType.APPROVED)
                                .build());
        arrangeOpenWorkflow(workflow, participant, 7L);

        assertThatThrownBy(() -> service.withdrawApproval(1L, 2L))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("claimed");

        workflow.getDecisions().clear();
        participant.setClaimedByUserId(7L);
        workflow.getDecisions()
                .add(
                        WorkflowDecision.builder()
                                .workflow(workflow)
                                .participantId(2L)
                                .userId(8L)
                                .decisionType(DecisionType.APPROVED)
                                .build());
        assertThatThrownBy(() -> service.withdrawApproval(1L, 2L))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("granted approval");
    }

    @Test
    void claimRejectsNonGroupAndAlreadyClaimedSlots() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 2L, null);
        participant.setPrincipalType(PrincipalType.USER);
        arrangeOpenWorkflow(workflow, participant, 7L);

        assertThatThrownBy(() -> service.claim(1L, 2L))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("organization-group");

        participant.setPrincipalType(PrincipalType.ORGANIZATION_GROUP);
        participant.setClaimedByUserId(8L);
        assertThatThrownBy(() -> service.claim(1L, 2L))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("already claimed");
    }

    @Test
    void decisionRequiresApproverPendingAndClaimedSlot() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 2L, 7L);
        arrangeOpenWorkflow(workflow, participant, 7L);

        participant.setRole(ParticipantRole.VIEWER);
        assertThatThrownBy(() -> service.decide(1L, decision(2L, DecisionType.APPROVED)))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("not an approver");

        participant.setRole(ParticipantRole.APPROVER);
        participant.setSlotStatus(ParticipantSlotStatus.APPROVED);
        assertThatThrownBy(() -> service.decide(1L, decision(2L, DecisionType.APPROVED)))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("not pending");

        participant.setSlotStatus(ParticipantSlotStatus.PENDING);
        participant.setClaimedByUserId(null);
        assertThatThrownBy(() -> service.decide(1L, decision(2L, DecisionType.APPROVED)))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("must be claimed");
    }

    @Test
    void openCreatesWorkflowResolvesParticipantsAndPublishesEvent() throws JacksonException {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.payloadType()).thenReturn(Object.class);
        when(handler.mode()).thenReturn(WorkflowMode.STATIC);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(20L);
        when(handler.additionalParticipants(any())).thenReturn(List.of());
        WorkflowDefinition definition =
                WorkflowDefinition.builder()
                        .enabled(true)
                        .staticApproversExclusive(false)
                        .selfApproveAllowed(false)
                        .staticOrganizations(new ArrayList<>())
                        .build();
        when(definitionService.resolveEffectiveDefinition(5L, 20L)).thenReturn(definition);
        when(jsonMapper.writeValueAsString(Map.of("key", "value")))
                .thenReturn("{\"key\":\"value\"}");
        when(workflowRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Workflow workflow = invocation.getArgument(0);
                            workflow.setId(1L);
                            return workflow;
                        });
        doAnswer(
                        invocation -> {
                            Workflow workflow = invocation.getArgument(0);
                            workflow.getParticipants()
                                    .add(
                                            WorkflowParticipant.builder()
                                                    .role(ParticipantRole.APPROVER)
                                                    .slotStatus(ParticipantSlotStatus.PENDING)
                                                    .build());
                            return null;
                        })
                .when(resolverService)
                .resolveParticipants(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(Boolean.class),
                        any(Boolean.class),
                        any(),
                        any());
        when(selfApproveService.isAllowed(any())).thenReturn(false);

        WorkflowDto result = service.open(5L, Map.of("key", "value"), "user", 99L);

        assertThat(result).isNotNull();
        verify(resolverService)
                .resolveParticipants(
                        any(Workflow.class),
                        eq(42L),
                        eq(5L),
                        eq(20L),
                        eq(List.of()),
                        eq(false),
                        eq(false),
                        eq(List.of()),
                        eq(List.of()));
        verify(handler).onOpened(any(Workflow.class));
        verify(eventPublisher).publish(any(Workflow.class), eq(WorkflowLifecycleEvent.OPENED));
    }

    @Test
    void openRejectsSecondConcurrentWorkflowWhenTypeDisallowsIt() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(false);
        when(workflowRepository.existsActiveByOpenedByUserIdAndWorkflowTypeId(42L, 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("already have an open workflow");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void openRejectsWhenUserHasNoOrganizationMembership() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.mayOpenWithoutActiveOrganizationMembership()).thenReturn(false);
        when(userOrganizationMembershipRepository.existsActiveByUser_Id(
                        eq(42L), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("No organization membership");
        verify(handler, never()).validateOpen(any());
    }

    @Test
    void openRejectsWhenOpenerMembershipRequiredAndMissing() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.requiresOpenerOrganizationMembership()).thenReturn(true);
        when(userOrganizationMembershipRepository.findActiveByUser_IdAndMainTrue(
                        eq(42L), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("No main organization membership");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void openRejectsDisabledWorkflowDefinition() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(20L);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(WorkflowDefinition.builder().enabled(false).build());

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void getWorkflowReturnsEnrichedDto() {
        Workflow workflow = workflow(1L);
        workflow.setOpenedByUserId(42L);
        when(workflowRepository.findWithDetailsById(1L)).thenReturn(Optional.of(workflow));
        when(selfApproveService.isAllowed(workflow)).thenReturn(true);
        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(
                                User.builder()
                                        .id(42L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .build()));
        when(mapper.toDto(workflow))
                .thenReturn(
                        WorkflowDto.builder()
                                .id(1L)
                                .openedByUserId(42L)
                                .participants(new ArrayList<>())
                                .decisions(new ArrayList<>())
                                .build());

        WorkflowDto result = service.getWorkflow(1L);

        assertThat(result.getOpenedByDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(result.isSelfApproveAllowed()).isTrue();
    }

    @Test
    void getWorkflowThrowsWhenMissing() {
        when(workflowRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkflow(99L))
                .isInstanceOf(WorkflowNotFoundException.class);
    }

    @Test
    void getDocumentPromotionPreviewDelegatesToPromotionService() {
        Workflow workflow = workflow(1L);
        WorkflowDocumentPromotionPreviewDto preview =
                WorkflowDocumentPromotionPreviewDto.builder().build();
        when(workflowRepository.findWithDetailsById(1L)).thenReturn(Optional.of(workflow));
        doReturn(handler).when(handlerRegistry).require(5L);
        when(documentPromotionService.buildPreview(workflow, handler)).thenReturn(preview);

        assertThat(service.getDocumentPromotionPreview(1L)).isSameAs(preview);
    }

    @Test
    void updateDocumentPromotionSelectionsPersistsWorkflowAndReturnsPreview() {
        Workflow workflow = workflow(1L);
        WorkflowDocumentPromotionPreviewDto preview =
                WorkflowDocumentPromotionPreviewDto.builder().build();
        UpdateWorkflowDocumentPromotionsRequest request =
                UpdateWorkflowDocumentPromotionsRequest.builder().selections(List.of()).build();
        when(workflowRepository.findWithDetailsById(1L)).thenReturn(Optional.of(workflow));
        doReturn(handler).when(handlerRegistry).require(5L);
        when(documentPromotionService.savePendingSelections(
                        workflow, handler, request.getSelections()))
                .thenReturn(preview);

        assertThat(service.updateDocumentPromotionSelections(1L, request)).isSameAs(preview);
        verify(workflowRepository).save(workflow);
    }

    @Test
    void transitionsRequireOpenWorkflow() {
        Workflow workflow = workflow(1L);
        workflow.setStatus(WorkflowStatus.CANCELLED);
        when(tokenService.requireUserId()).thenReturn(42L);
        when(workflowRepository.findWithDetailsById(1L)).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void listOpenedForCurrentUserReturnsMappedWorkflows() {
        Workflow workflow = workflow(1L);
        when(tokenService.requireUserId()).thenReturn(42L);
        when(workflowRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(workflow)));
        when(workflowRepository.findWithDetailsByIdIn(List.of(1L))).thenReturn(List.of(workflow));
        when(selfApproveService.isAllowed(workflow)).thenReturn(false);
        when(mapper.toDtoList(List.of(workflow)))
                .thenReturn(
                        List.of(
                                WorkflowDto.builder()
                                        .id(1L)
                                        .openedByUserId(42L)
                                        .participants(new ArrayList<>())
                                        .decisions(new ArrayList<>())
                                        .build()));

        assertThat(
                        service.listOpenedForCurrentUser(
                                        new ListQuery(0, 50, null, null),
                                        WorkflowListFilter.parse(null, null))
                                .getContent())
                .singleElement()
                .satisfies(
                        dto -> {
                            assertThat(dto.getId()).isEqualTo(1L);
                            assertThat(dto.isSelfApproveAllowed()).isFalse();
                        });
    }

    @Test
    void listParticipationsExcludesOwnWorkflowUnlessSelfApproveAllowed() {
        Workflow ownWorkflow = workflow(1L);
        ownWorkflow.setOpenedByUserId(42L);
        Workflow otherWorkflow = workflow(2L);
        otherWorkflow.setOpenedByUserId(99L);
        when(tokenService.requireUserId()).thenReturn(42L);
        when(membershipRepository.findByUser_Id(42L)).thenReturn(List.of());
        when(workflowRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ownWorkflow, otherWorkflow));
        when(workflowRepository.findWithDetailsByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(ownWorkflow, otherWorkflow));
        when(selfApproveService.isAllowed(ownWorkflow)).thenReturn(false);
        when(workflowInstanceAccess.requiresParticipantAction(otherWorkflow, 42L)).thenReturn(true);
        when(mapper.toDtoList(List.of(otherWorkflow)))
                .thenReturn(
                        List.of(
                                WorkflowDto.builder()
                                        .id(2L)
                                        .participants(new ArrayList<>())
                                        .decisions(new ArrayList<>())
                                        .build()));

        assertThat(
                        service.listParticipationsForCurrentUser(
                                        new ListQuery(0, 50, null, null),
                                        WorkflowListFilter.parse(null, null))
                                .getContent())
                .extracting(WorkflowDto::getId)
                .containsExactly(2L);
    }

    @Test
    void listParticipationsForwardsClientSortToRepository() {
        Workflow workflow = workflow(2L);
        workflow.setOpenedByUserId(99L);
        when(tokenService.requireUserId()).thenReturn(42L);
        when(membershipRepository.findByUser_Id(42L)).thenReturn(List.of());
        when(workflowRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(workflow));
        when(workflowRepository.findWithDetailsByIdIn(List.of(2L))).thenReturn(List.of(workflow));
        when(workflowInstanceAccess.requiresParticipantAction(workflow, 42L)).thenReturn(true);
        when(mapper.toDtoList(List.of(workflow)))
                .thenReturn(
                        List.of(
                                WorkflowDto.builder()
                                        .id(2L)
                                        .participants(new ArrayList<>())
                                        .decisions(new ArrayList<>())
                                        .build()));

        service.listParticipationsForCurrentUser(
                new ListQuery(0, 50, null, "openedAt,asc"), WorkflowListFilter.parse(null, null));

        org.mockito.ArgumentCaptor<Sort> sort = org.mockito.ArgumentCaptor.forClass(Sort.class);
        verify(workflowRepository).findAll(any(Specification.class), sort.capture());
        assertThat(sort.getValue().getOrderFor("openedAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void openRejectsMissingPermissionInContextOrganization() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(20L);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.of(PermissionType.USER_WRITE));
        when(organizationAccessService.hasOrganizationPermission(20L, PermissionType.USER_WRITE))
                .thenReturn(false);

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowAccessDeniedException.class)
                .hasMessageContaining("Missing permission");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void partialApprovalLeavesWorkflowOpenUntilAllSlotsApprove() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant first = participant(workflow, 10L, 42L);
        WorkflowParticipant second = participant(workflow, 11L, 43L);
        workflow.getParticipants().add(first);
        workflow.getParticipants().add(second);
        arrangeOpenWorkflow(workflow, first, 42L);

        service.decide(1L, decision(10L, DecisionType.APPROVED));

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.OPEN);
        assertThat(first.getSlotStatus()).isEqualTo(ParticipantSlotStatus.APPROVED);
        assertThat(second.getSlotStatus()).isEqualTo(ParticipantSlotStatus.PENDING);
        verify(handler, never()).onApproved(workflow);
        verify(eventPublisher, never()).publish(workflow, WorkflowLifecycleEvent.APPROVED);
    }

    @Test
    void openRejectsWhenHandlerDoesNotResolveContextOrganization() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.resolveContextOrganization(any())).thenReturn(null);

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("context organization");
    }

    @Test
    void openRejectsWhenHandlerDoesNotResolvePermissionCheckOrganization() {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(null);

        assertThatThrownBy(() -> service.open(5L, null, null, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("permission-check organization");
    }

    @Test
    void openPassesDynamicOrganizationsToResolver() throws JacksonException {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.mode()).thenReturn(WorkflowMode.DYNAMIC);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(20L);
        when(handler.resolveDynamicOrganizations(any()))
                .thenReturn(List.of(new ApproverOrg(25L), new ApproverOrg(26L)));
        when(handler.additionalParticipants(any())).thenReturn(List.of());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(
                        WorkflowDefinition.builder()
                                .enabled(true)
                                .staticApproversExclusive(false)
                                .selfApproveAllowed(false)
                                .staticOrganizations(new ArrayList<>())
                                .build());
        when(workflowRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Workflow workflow = invocation.getArgument(0);
                            workflow.setId(1L);
                            return workflow;
                        });
        when(selfApproveService.isAllowed(any())).thenReturn(false);

        service.open(5L, null, null, null);

        verify(resolverService)
                .resolveParticipants(
                        any(Workflow.class),
                        eq(42L),
                        eq(5L),
                        eq(20L),
                        eq(List.of()),
                        eq(false),
                        eq(false),
                        eq(List.of(25L, 26L)),
                        eq(List.of()));
    }

    @Test
    void openRecordsAutoApproveDecisionsAndCompletesWhenAllSlotsPreApproved()
            throws JacksonException {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.mode()).thenReturn(WorkflowMode.STATIC);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(20L);
        when(handler.additionalParticipants(any())).thenReturn(List.of());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(
                        WorkflowDefinition.builder()
                                .enabled(true)
                                .staticApproversExclusive(false)
                                .selfApproveAllowed(false)
                                .staticOrganizations(new ArrayList<>())
                                .build());
        doAnswer(
                        invocation -> {
                            Workflow workflow = invocation.getArgument(0);
                            workflow.getParticipants()
                                    .add(
                                            WorkflowParticipant.builder()
                                                    .id(10L)
                                                    .role(ParticipantRole.APPROVER)
                                                    .slotStatus(ParticipantSlotStatus.APPROVED)
                                                    .build());
                            return null;
                        })
                .when(resolverService)
                .resolveParticipants(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(Boolean.class),
                        any(Boolean.class),
                        any(),
                        any());
        when(workflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentPromotionService.resolvePromotionsForCompletion(any(), any()))
                .thenReturn(List.of());
        when(documentPromotionService.promoteToUserDocuments(any(), any(), any()))
                .thenReturn(List.of());
        when(selfApproveService.isAllowed(any())).thenReturn(false);

        service.open(5L, null, null, null);

        verify(workflowRepository, atLeast(2)).save(any());
        verify(handler).onApproved(any(Workflow.class));
        verify(eventPublisher, never())
                .publish(any(Workflow.class), eq(WorkflowLifecycleEvent.OPENED));
        verify(eventPublisher).publish(any(Workflow.class), eq(WorkflowLifecycleEvent.APPROVED));
    }

    @Test
    void openRejectsUnserializablePayload() throws JacksonException {
        when(tokenService.requireUserId()).thenReturn(42L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(handler.payloadType()).thenReturn(Object.class);
        when(handler.mode()).thenReturn(WorkflowMode.STATIC);
        when(handler.requiredPermissionToOpen()).thenReturn(Optional.empty());
        when(handler.resolveContextOrganization(any())).thenReturn(20L);
        when(handler.resolvePermissionCheckOrganization(
                        org.mockito.ArgumentMatchers.<WorkflowOpenContext<Object>>any(), eq(20L)))
                .thenReturn(20L);
        when(handler.additionalParticipants(any())).thenReturn(List.of());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(
                        WorkflowDefinition.builder()
                                .enabled(true)
                                .staticOrganizations(new ArrayList<>())
                                .build());
        JacksonException serializationError = org.mockito.Mockito.mock(JacksonException.class);
        when(serializationError.getMessage()).thenReturn("cannot serialize");
        when(jsonMapper.writeValueAsString(any())).thenThrow(serializationError);

        assertThatThrownBy(() -> service.open(5L, Map.of("bad", "payload"), null, null))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("serialize");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    void getWorkflowEnrichesParticipantsAndDecisions() {
        Workflow workflow = workflow(1L);
        workflow.setOpenedByUserId(42L);
        WorkflowParticipantDto groupParticipant =
                WorkflowParticipantDto.builder()
                        .id(10L)
                        .principalType(PrincipalType.ORGANIZATION_GROUP)
                        .principalId(30L)
                        .organizationId(20L)
                        .slotStatus(ParticipantSlotStatus.PENDING)
                        .build();
        WorkflowParticipantDto userParticipant =
                WorkflowParticipantDto.builder()
                        .id(11L)
                        .principalType(PrincipalType.USER)
                        .principalId(50L)
                        .claimedByUserId(51L)
                        .build();
        WorkflowDecisionDto decisionDto = WorkflowDecisionDto.builder().userId(51L).build();
        when(workflowRepository.findWithDetailsById(1L)).thenReturn(Optional.of(workflow));
        when(selfApproveService.isAllowed(workflow)).thenReturn(false);
        when(mapper.toDto(workflow))
                .thenReturn(
                        WorkflowDto.builder()
                                .id(1L)
                                .openedByUserId(42L)
                                .participants(
                                        new ArrayList<>(List.of(groupParticipant, userParticipant)))
                                .decisions(new ArrayList<>(List.of(decisionDto)))
                                .build());
        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(
                                User.builder()
                                        .id(42L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .build(),
                                User.builder()
                                        .id(50L)
                                        .firstName("Grace")
                                        .lastName("Hopper")
                                        .build(),
                                User.builder()
                                        .id(51L)
                                        .firstName("Alan")
                                        .lastName("Turing")
                                        .build()));
        when(organizationGroupRepository.findAllById(Set.of(30L)))
                .thenReturn(
                        List.of(
                                OrganizationGroup.builder()
                                        .id(30L)
                                        .name("Führung")
                                        .type(OrganizationGroupType.LEADERSHIP)
                                        .tenantId(7L)
                                        .build()));
        when(organizationRepository.findAllById(Set.of(20L)))
                .thenReturn(List.of(Organization.builder().id(20L).name("Fire Dept").build()));

        WorkflowDto result = service.getWorkflow(1L);

        assertThat(result.getOpenedByDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(result.getParticipants().get(0).getOrganizationDisplayName())
                .isEqualTo("Fire Dept");
        assertThat(result.getParticipants().get(0).getPrincipalDisplayName()).isEqualTo("Führung");
        assertThat(result.getParticipants().get(1).getPrincipalDisplayName())
                .isEqualTo("Grace Hopper");
        assertThat(result.getParticipants().get(1).getClaimedByDisplayName())
                .isEqualTo("Alan Turing");
        assertThat(result.getDecisions().getFirst().getUserDisplayName()).isEqualTo("Alan Turing");
    }

    @Test
    void listParticipationsIncludesOwnWorkflowWhenSelfApproveAllowed() {
        Workflow ownWorkflow = workflow(1L);
        ownWorkflow.setOpenedByUserId(42L);
        when(tokenService.requireUserId()).thenReturn(42L);
        when(membershipRepository.findByUser_Id(42L)).thenReturn(List.of());
        when(workflowRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ownWorkflow));
        when(workflowRepository.findWithDetailsByIdIn(List.of(1L)))
                .thenReturn(List.of(ownWorkflow));
        when(selfApproveService.isAllowed(ownWorkflow)).thenReturn(true);
        when(workflowInstanceAccess.requiresParticipantAction(ownWorkflow, 42L)).thenReturn(true);
        when(mapper.toDtoList(List.of(ownWorkflow)))
                .thenReturn(
                        List.of(
                                WorkflowDto.builder()
                                        .id(1L)
                                        .participants(new ArrayList<>())
                                        .decisions(new ArrayList<>())
                                        .build()));

        assertThat(
                        service.listParticipationsForCurrentUser(
                                        new ListQuery(0, 50, null, null),
                                        WorkflowListFilter.parse(null, null))
                                .getContent())
                .extracting(WorkflowDto::getId)
                .containsExactly(1L);
    }

    @Test
    void listForOrganization_returnsOpenWorkflowsForTenantOrganization() {
        Workflow open = workflow(9L);
        WorkflowDto dto = WorkflowDto.builder().id(9L).status(WorkflowStatus.OPEN).build();

        when(tokenService.requireTenantId()).thenReturn(7L);
        when(organizationAccessService.organizationNotInTenant(20L, 7L)).thenReturn(false);
        when(workflowRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(open)));
        when(workflowRepository.findWithDetailsByIdIn(List.of(9L))).thenReturn(List.of(open));
        when(mapper.toDtoList(List.of(open))).thenReturn(List.of(dto));

        List<WorkflowDto> result =
                service.listForOrganization(
                                20L,
                                new ListQuery(0, 50, null, null),
                                WorkflowListFilter.parse(null, null))
                        .getContent();

        assertThat(result).containsExactly(dto);
        verify(workflowRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listForOrganization_returnsEmptyWhenOrganizationNotInTenant() {
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(organizationAccessService.organizationNotInTenant(20L, 7L)).thenReturn(true);

        assertThat(
                        service.listForOrganization(
                                        20L,
                                        new ListQuery(0, 50, null, null),
                                        WorkflowListFilter.parse(null, null))
                                .getContent())
                .isEmpty();
        verify(workflowRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void reconcileOpenInstancesAfterConfigurationChangeUpdatesApproverGroupAndRefreshesTodos() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, 42L);
        workflow.getParticipants().add(participant);

        arrangeReconcileWorkflow(workflow, 20L);
        when(mappingService.requireMapping(20L, 5L))
                .thenReturn(
                        WorkflowTypeMapping.builder()
                                .organizationGroupId(99L)
                                .autoApprove(false)
                                .build());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(
                        WorkflowDefinition.builder()
                                .selfApproveAllowed(true)
                                .staticOrganizations(new ArrayList<>())
                                .build());

        service.reconcileOpenInstancesAfterConfigurationChange(
                WorkflowConfigurationChangedEvent.forOrganizations(7L, 5L, Set.of(20L)));

        assertThat(participant.getPrincipalId()).isEqualTo(99L);
        verify(participantClaimService).releasePendingClaim(participant, 42L);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.OPENED);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.UPDATED);
    }

    @Test
    void
            reconcileOpenInstancesAfterConfigurationChangeAutoApprovesPendingSlotAndCompletesWorkflow() {
        Workflow workflow = workflow(1L);
        WorkflowParticipant participant = participant(workflow, 10L, null);
        participant.setClaimedByUserId(null);
        workflow.getParticipants().add(participant);

        arrangeReconcileWorkflow(workflow, 20L);
        when(mappingService.requireMapping(20L, 5L))
                .thenReturn(
                        WorkflowTypeMapping.builder()
                                .organizationGroupId(30L)
                                .autoApprove(true)
                                .build());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(
                        WorkflowDefinition.builder()
                                .selfApproveAllowed(false)
                                .staticOrganizations(new ArrayList<>())
                                .build());
        when(documentPromotionService.resolvePromotionsForCompletion(any(), any()))
                .thenReturn(List.of());
        when(documentPromotionService.promoteToUserDocuments(any(), any(), any()))
                .thenReturn(List.of());

        service.reconcileOpenInstancesAfterConfigurationChange(
                WorkflowConfigurationChangedEvent.forOrganizations(7L, 5L, Set.of(20L)));

        assertThat(participant.getSlotStatus()).isEqualTo(ParticipantSlotStatus.APPROVED);
        assertThat(workflow.getDecisions())
                .singleElement()
                .satisfies(
                        decision -> {
                            assertThat(decision.getParticipantId()).isEqualTo(10L);
                            assertThat(decision.getUserId()).isNull();
                            assertThat(decision.getDecisionType()).isEqualTo(DecisionType.APPROVED);
                        });
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.APPROVED);
        verify(handler).onApproved(workflow);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.APPROVED);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.UPDATED);
        verify(eventPublisher, never()).publish(workflow, WorkflowLifecycleEvent.OPENED);
    }

    @Test
    void reconcileOpenInstancesAfterConfigurationChangeReleasesSelfClaimWhenSelfApproveDisabled() {
        Workflow workflow = workflow(1L);
        workflow.setOpenedByUserId(42L);
        WorkflowParticipant participant = participant(workflow, 10L, 42L);
        workflow.getParticipants().add(participant);

        arrangeReconcileWorkflow(workflow, 20L);
        when(mappingService.requireMapping(20L, 5L))
                .thenReturn(
                        WorkflowTypeMapping.builder()
                                .organizationGroupId(30L)
                                .autoApprove(false)
                                .build());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(
                        WorkflowDefinition.builder()
                                .selfApproveAllowed(false)
                                .staticOrganizations(new ArrayList<>())
                                .build());

        service.reconcileOpenInstancesAfterConfigurationChange(
                WorkflowConfigurationChangedEvent.forOrganizations(7L, 5L, Set.of(20L)));

        verify(participantClaimService).releasePendingClaim(participant, 42L);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.OPENED);
        verify(eventPublisher).publish(workflow, WorkflowLifecycleEvent.UPDATED);
    }

    private void arrangeReconcileWorkflow(Workflow workflow, long contextOrgId) {
        doReturn(handler).when(handlerRegistry).require(5L);
        when(workflowRepository.findOpenWithDetailsByWorkflowTypeId(5L))
                .thenReturn(List.of(workflow));
        lenient().when(handler.mode()).thenReturn(WorkflowMode.STATIC);
        lenient().when(handler.workflowTypeId()).thenReturn(5L);
        lenient().when(handler.payloadType()).thenReturn(Object.class);
        lenient().when(handler.resolveContextOrganization(any())).thenReturn(contextOrgId);
        lenient().when(handler.resolveDynamicOrganizations(any())).thenReturn(List.of());
        lenient().when(handler.additionalParticipants(any())).thenReturn(List.of());
        lenient()
                .when(
                        resolverService.syncOrgApproverSlots(
                                eq(workflow), eq(5L), eq(contextOrgId), any(), anyBoolean(), any()))
                .thenReturn(
                        new WorkflowResolverService.OrgApproverSlotSyncResult(false, List.of()));
        lenient().when(workflowRepository.save(workflow)).thenReturn(workflow);
        lenient()
                .when(workflowRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(workflow));
    }

    @Test
    void updatePayloadRejectsPayloadViolatingBeanValidationConstraints() {
        Workflow workflow = workflow(1L);
        arrangeWorkflow(workflow, 42L);
        doReturn(UserDocumentSubmissionWorkflowPayload.class).when(handler).payloadType();
        UserDocumentSubmissionWorkflowPayload payload =
                UserDocumentSubmissionWorkflowPayload.builder()
                        .title("x".repeat(513))
                        .attachmentIds(List.of(1L))
                        .build();

        assertThatThrownBy(() -> service.updatePayload(1L, payload))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("title");
        verify(workflowRepository, never()).save(any());
    }

    private void arrangeOpenWorkflow(
            Workflow workflow, WorkflowParticipant participant, long currentUserId) {
        arrangeWorkflowAndParticipant(workflow, participant, currentUserId);
    }

    private void arrangeWorkflowAndParticipant(
            Workflow workflow, WorkflowParticipant participant, long currentUserId) {
        arrangeWorkflow(workflow, currentUserId);
        when(participantRepository.findByIdAndWorkflow_Id(participant.getId(), workflow.getId()))
                .thenReturn(Optional.of(participant));
    }

    private void arrangeWorkflow(Workflow workflow, long currentUserId) {
        when(tokenService.requireUserId()).thenReturn(currentUserId);
        when(workflowRepository.findWithDetailsById(workflow.getId()))
                .thenReturn(Optional.of(workflow));
        lenient().doReturn(handler).when(handlerRegistry).require(workflow.getWorkflowTypeId());
        lenient().when(handler.allowsInputChangeRequests()).thenReturn(true);
        lenient().when(handler.allowsOpenerFreeInputEdit()).thenReturn(true);
        lenient().when(handler.mapsRejectToInputChange()).thenReturn(false);
        lenient()
                .when(documentPromotionService.resolvePromotionsForCompletion(any(), any()))
                .thenReturn(List.of());
        lenient()
                .when(documentPromotionService.promoteToUserDocuments(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void cancelStaleWorkflowCancelsLockedWorkflowOnce() {
        Workflow stale =
                Workflow.builder()
                        .id(9L)
                        .tenantId(7L)
                        .workflowTypeId(5L)
                        .status(WorkflowStatus.OPEN)
                        .openedAt(Instant.parse("2020-01-01T00:00:00Z"))
                        .openedByUserId(42L)
                        .participants(new LinkedHashSet<>())
                        .decisions(new LinkedHashSet<>())
                        .inputChanges(new LinkedHashSet<>())
                        .build();
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(workflowRepository.findAndLockStaleOpenWorkflow(9L, cutoff))
                .thenReturn(Optional.of(stale));

        boolean cancelled = service.cancelStaleWorkflow(9L, cutoff);

        assertThat(cancelled).isTrue();
        assertThat(stale.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(stale.getDecidedAt()).isNotNull();
        verify(handler).onCancelled(stale);
        verify(eventPublisher, times(1))
                .publish(
                        stale,
                        WorkflowLifecycleEvent.CANCELLED,
                        WorkflowEventContext.ofActor(null, null));
        verify(openExclusivityService).releaseLocks(9L);
    }

    @Test
    void cancelStaleWorkflowSkipsWorkflowLockedOrClosedElsewhere() {
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(workflowRepository.findAndLockStaleOpenWorkflow(9L, cutoff))
                .thenReturn(Optional.empty());

        assertThat(service.cancelStaleWorkflow(9L, cutoff)).isFalse();

        verify(workflowRepository, never()).save(any());
        verifyNoInteractions(eventPublisher, openExclusivityService);
    }

    @Test
    void cancelActiveWorkflowsClosesOnlyActiveOnesWithoutHandlerCallback() {
        Workflow open = workflow(1L);
        Workflow awaiting = workflow(2L);
        awaiting.setStatus(WorkflowStatus.AWAITING_INPUT_CHANGE);
        Workflow approved = workflow(3L);
        approved.setStatus(WorkflowStatus.APPROVED);
        when(workflowRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(open, awaiting, approved));

        service.cancelActiveWorkflows(List.of(1L, 2L, 3L));

        assertThat(open.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(awaiting.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(approved.getStatus()).isEqualTo(WorkflowStatus.APPROVED);
        verify(handler, never()).onCancelled(any());
        verify(openExclusivityService).releaseLocks(1L);
        verify(openExclusivityService).releaseLocks(2L);
        verify(openExclusivityService, never()).releaseLocks(3L);
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void cancelActiveWorkflowsIgnoresEmptyInput() {
        service.cancelActiveWorkflows(List.of());

        verifyNoInteractions(workflowRepository, eventPublisher);
    }

    private Workflow workflow(long id) {
        return Workflow.builder()
                .id(id)
                .tenantId(7L)
                .workflowTypeId(5L)
                .status(WorkflowStatus.OPEN)
                .openedAt(Instant.now())
                .openedByUserId(42L)
                .participants(new LinkedHashSet<>())
                .decisions(new LinkedHashSet<>())
                .inputChanges(new LinkedHashSet<>())
                .build();
    }

    private WorkflowParticipant participant(Workflow workflow, long id, Long claimedBy) {
        return WorkflowParticipant.builder()
                .id(id)
                .tenantId(7L)
                .workflow(workflow)
                .role(ParticipantRole.APPROVER)
                .requirement(ParticipantRequirement.MANDATORY)
                .principalType(PrincipalType.ORGANIZATION_GROUP)
                .principalId(30L)
                .organizationId(20L)
                .slotKey("approver")
                .slotStatus(ParticipantSlotStatus.PENDING)
                .claimedByUserId(claimedBy)
                .build();
    }

    private WorkflowDecisionRequest decision(long participantId, DecisionType decisionType) {
        return WorkflowDecisionRequest.builder()
                .participantId(participantId)
                .decision(decisionType)
                .comment("Looks good")
                .build();
    }
}
