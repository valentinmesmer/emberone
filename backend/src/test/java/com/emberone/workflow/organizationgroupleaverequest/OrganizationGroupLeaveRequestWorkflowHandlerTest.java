/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationgroupleaverequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.dto.OrganizationGroupLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
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
class OrganizationGroupLeaveRequestWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock UserOrganizationGroupMembershipService groupMembershipService;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock WorkflowTypeMappingService workflowTypeMappingService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private OrganizationGroupLeaveRequestWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new OrganizationGroupLeaveRequestWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        groupMembershipService,
                        groupMembershipRepository,
                        organizationGroupRepository,
                        workflowTypeMappingService,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesStaticLeaveMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.STATIC);
        assertThat(handler.payloadType())
                .isEqualTo(OrganizationGroupLeaveRequestWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
    }

    @Test
    void resolveContextOrganizationUsesSelectedOrganization() {
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                OrganizationGroupLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .organizationGroupId(30L)
                        .build();

        assertThat(handler.resolveContextOrganization(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isEqualTo(20L);
    }

    @Test
    void validateOpenNormalizesPayloadAndChecksMembership() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization =
                Organization.builder().id(20L).tenantId(7L).name("Fire Dept").build();
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(30L)
                        .tenantId(7L)
                        .name("Crew")
                        .type(OrganizationGroupType.CUSTOM)
                        .organization(organization)
                        .archived(false)
                        .build();
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                OrganizationGroupLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .organizationGroupId(30L)
                        .reason("  Stepping down  ")
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationGroupRepository.findById(30L)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.findActiveByUser_IdAndOrganizationGroup_Id(
                        eq(1L), eq(30L), any(Instant.class)))
                .thenReturn(Optional.of(UserOrganizationGroupMembership.builder().id(8L).build()));
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getOrganizationName()).isEqualTo("Fire Dept");
        assertThat(payload.getOrganizationGroupName()).isEqualTo("Crew");
        assertThat(payload.getReason()).isEqualTo("Stepping down");
    }

    @Test
    void validateOpenRejectsAllMembersGroup() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization = Organization.builder().id(20L).tenantId(7L).name("Org").build();
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(30L)
                        .tenantId(7L)
                        .name("All")
                        .type(OrganizationGroupType.ALL_MEMBERS)
                        .organization(organization)
                        .archived(false)
                        .build();
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                OrganizationGroupLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .organizationGroupId(30L)
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationGroupRepository.findById(30L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("all-members");
    }

    @Test
    void validateOpenRejectsLeadershipGroup() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization = Organization.builder().id(20L).tenantId(7L).name("Org").build();
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(30L)
                        .tenantId(7L)
                        .name("Führung")
                        .type(OrganizationGroupType.LEADERSHIP)
                        .organization(organization)
                        .archived(false)
                        .build();
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                OrganizationGroupLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .organizationGroupId(30L)
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationGroupRepository.findById(30L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting(ex -> ((WorkflowValidationException) ex).getCode())
                .isEqualTo(WorkflowValidationException.CODE_LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED);
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsOrganizationGroupKeyAfterValidateOpen() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization organization = Organization.builder().id(20L).tenantId(7L).name("Org").build();
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(30L)
                        .tenantId(7L)
                        .name("Crew")
                        .type(OrganizationGroupType.CUSTOM)
                        .organization(organization)
                        .archived(false)
                        .build();
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                OrganizationGroupLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .organizationGroupId(30L)
                        .build();
        WorkflowOpenContext<OrganizationGroupLeaveRequestWorkflowPayload> ctx =
                new WorkflowOpenContext<>(1L, 7L, payload);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationGroupRepository.findById(30L)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.findActiveByUser_IdAndOrganizationGroup_Id(
                        eq(1L), eq(30L), any(Instant.class)))
                .thenReturn(Optional.of(UserOrganizationGroupMembership.builder().id(8L).build()));
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));

        handler.validateOpen(ctx);

        assertThat(handler.resolveOpenConcurrencyKeys(ctx, null))
                .containsExactly(WorkflowOpenConcurrencyKeys.organizationGroup(30L));
    }

    @Test
    void onApprovedEndsGroupMembership() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .openedByUserId(1L)
                        .payload("{\"organizationId\":20,\"organizationGroupId\":30}")
                        .build();

        handler.onApproved(workflow);

        verify(groupMembershipService).endActiveMembership(eq(1L), eq(30L), any(Instant.class));
    }

    @Test
    void onOpenedAttachesDocuments() {
        Workflow workflow = Workflow.builder().id(3L).build();
        handler.onOpened(workflow);
        verify(workflowDocumentService)
                .attachOrganizationGroupLeaveRequestAttachmentsToWorkflow(workflow);
    }

    @Test
    void resolveDocumentPromotionMetadataInterpolatesOrganizationGroupName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(7L)
                        .payload(
                                "{\"organizationId\":20,\"organizationGroupId\":30,\"organizationGroupName\":\"Atemschutz\"}")
                        .build();
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.organizationGroupLeaveRequest",
                        Map.of("name", "Atemschutz")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Gruppenaustritt Atemschutz")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Gruppenaustritt Atemschutz");
    }
}
