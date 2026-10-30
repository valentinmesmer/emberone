/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationleaverequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserAccountContinuity;
import com.emberone.user.service.UserDeactivationService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.OrganizationMembershipExitService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.dto.OrganizationLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import com.emberone.workflow.spi.WorkflowOpenExclusivityKeys;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OrganizationLeaveRequestWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock OrganizationRepository organizationRepository;
    @Mock WorkflowTypeMappingService workflowTypeMappingService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;
    @Mock AllMembersGroupService allMembersGroupService;
    @Mock UserAccountContinuity userAccountContinuity;
    @Mock UserDeactivationService userDeactivationService;
    @Mock OrganizationMembershipExitService organizationMembershipExitService;

    private OrganizationLeaveRequestWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new OrganizationLeaveRequestWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        organizationRepository,
                        workflowTypeMappingService,
                        allMembersGroupService,
                        userAccountContinuity,
                        userDeactivationService,
                        organizationMembershipExitService,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesStaticLeaveMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.ORGANIZATION_LEAVE_REQUEST);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.STATIC);
        assertThat(handler.payloadType()).isEqualTo(OrganizationLeaveRequestWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
    }

    @Test
    void resolveContextOrganizationUsesSelectedOrganization() {
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder().organizationId(20L).build();

        assertThat(handler.resolveContextOrganization(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isEqualTo(20L);
    }

    @Test
    void validateOpenNormalizesPayloadAndChecksMembership() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(20L).tenantId(7L).name("Target Fire Dept").build();
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .reason("  Leaving  ")
                        .build();
        UserOrganizationMembership activeMembership =
                UserOrganizationMembership.builder()
                        .organization(organization)
                        .main(false)
                        .startDate(Instant.now().minusSeconds(86400))
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(Optional.of(activeMembership));
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());
        when(userDeactivationService.hasDeactivationPending(1L)).thenReturn(false);
        doNothing()
                .when(userAccountContinuity)
                .requireSustainableAfterEndingOrganizationMembershipForWorkflow(
                        eq(1L), eq(20L), any(Instant.class));

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getOrganizationName()).isEqualTo("Target Fire Dept");
        assertThat(payload.getReason()).isEqualTo("Leaving");
    }

    @Test
    void validateOpenRejectsWhenReasonMissing() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder().organizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .main(false)
                                        .startDate(Instant.now().minusSeconds(86400))
                                        .build()));
        when(userDeactivationService.hasDeactivationPending(1L)).thenReturn(false);
        doNothing()
                .when(userAccountContinuity)
                .requireSustainableAfterEndingOrganizationMembershipForWorkflow(
                        eq(1L), eq(20L), any(Instant.class));

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Reason is required");
    }

    @Test
    void validateOpenRejectsWhenNotAMember() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder().organizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not an active member");
    }

    @Test
    void resolveOpenExclusivityKeysUsesMemberAndOrganization() {
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder().organizationId(20L).build();
        WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx =
                new WorkflowOpenContext<>(1L, 7L, payload);

        assertThat(handler.resolveOpenExclusivityKeys(ctx, null))
                .containsExactly(WorkflowOpenExclusivityKeys.organizationMembershipExit(1L, 20L));
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsOrganizationKeyAfterValidateOpen() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .reason("Leaving")
                        .build();
        WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> ctx =
                new WorkflowOpenContext<>(1L, 7L, payload);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .main(false)
                                        .startDate(Instant.now().minusSeconds(86400))
                                        .build()));
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());
        when(userDeactivationService.hasDeactivationPending(1L)).thenReturn(false);
        doNothing()
                .when(userAccountContinuity)
                .requireSustainableAfterEndingOrganizationMembershipForWorkflow(
                        eq(1L), eq(20L), any(Instant.class));

        handler.validateOpen(ctx);

        assertThat(handler.resolveOpenConcurrencyKeys(ctx, null))
                .containsExactly(WorkflowOpenConcurrencyKeys.organization(20L));
    }

    @Test
    void validateOpenRejectsWhenUserIsInLeadershipGroup() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        OrganizationLeaveRequestWorkflowPayload payload =
                OrganizationLeaveRequestWorkflowPayload.builder().organizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .main(false)
                                        .startDate(Instant.now().minusSeconds(86400))
                                        .build()));
        when(allMembersGroupService.hasActiveLeadershipMembership(1L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting(ex -> ((WorkflowValidationException) ex).getCode())
                .isEqualTo(WorkflowValidationException.CODE_LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE);
    }

    @Test
    void onApprovedEndsMembership() {
        Organization organization = Organization.builder().id(20L).build();
        UserOrganizationMembership membership =
                UserOrganizationMembership.builder()
                        .id(40L)
                        .organization(organization)
                        .main(false)
                        .build();
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(Optional.of(membership));
        when(allMembersGroupService.hasActiveLeadershipMembership(1L, 20L)).thenReturn(false);
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .openedByUserId(1L)
                        .payload("{\"organizationId\":20,\"organizationName\":\"Target\"}")
                        .build();

        handler.onApproved(workflow);

        verify(organizationMembershipExitService).applyApprovedExit(membership);
    }

    @Test
    void onOpenedAttachesDocuments() {
        Workflow workflow = Workflow.builder().id(3L).build();
        handler.onOpened(workflow);
        verify(workflowDocumentService)
                .attachOrganizationLeaveRequestAttachmentsToWorkflow(workflow);
    }

    @Test
    void resolveDocumentPromotionMetadataInterpolatesOrganizationName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(7L)
                        .payload("{\"organizationId\":20,\"organizationName\":\"FF Musterstadt\"}")
                        .build();
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.organizationLeaveRequest",
                        Map.of("name", "FF Musterstadt")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Organisationsaustritt FF Musterstadt")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Organisationsaustritt FF Musterstadt");
    }
}
