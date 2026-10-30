/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationjoinrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.dto.OrganizationJoinRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.ApproverOrg;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OrganizationJoinRequestWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock OnboardingInvitationService onboardingInvitationService;
    @Mock UserService userService;
    @Mock OrganizationRepository organizationRepository;
    @Mock WorkflowTypeMappingService workflowTypeMappingService;
    @Mock WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private OrganizationJoinRequestWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new OrganizationJoinRequestWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        onboardingInvitationService,
                        userService,
                        organizationRepository,
                        workflowTypeMappingService,
                        workflowDefinitionRepository,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesDynamicOrganizationJoinMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.ORGANIZATION_JOIN_REQUEST);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.DYNAMIC);
        assertThat(handler.payloadType()).isEqualTo(OrganizationJoinRequestWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
        assertThat(handler.mayOpenWithoutActiveOrganizationMembership()).isTrue();
        assertThat(handler.listedInBeginnableCatalog()).isFalse();
    }

    @Test
    void resolveDynamicOrganizationsReturnsTargetOrganization() {
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(25L).build();

        List<ApproverOrg> orgs =
                handler.resolveDynamicOrganizations(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(orgs).containsExactly(new ApproverOrg(25L));
    }

    @Test
    void resolveInstanceOrganizationPrefersTargetOverCurrentMembership() {
        Workflow workflow =
                Workflow.builder()
                        .id(4L)
                        .tenantId(7L)
                        .openedByUserId(2L)
                        .payload(
                                "{\"targetOrganizationId\":1,\"targetOrganizationName\":\"Feuerwehr Teststadt\"}")
                        .build();

        assertThat(handler.resolveInstanceOrganization(workflow)).isEqualTo(1L);
        verify(membershipRepository, never())
                .findActiveByUser_IdAndMainTrue(anyLong(), any(Instant.class));
    }

    @Test
    void validateOpenNormalizesPayloadAndChecksTargetOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        Organization targetOrg =
                Organization.builder().id(20L).tenantId(7L).name("Target Fire Dept").build();
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder()
                        .targetOrganizationId(20L)
                        .reason("  Second org  ")
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(targetOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getTargetOrganizationName()).isEqualTo("Target Fire Dept");
        assertThat(payload.getReason()).isEqualTo("Second org");
    }

    @Test
    void validateOpenRejectsTargetEqualToMainOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(10L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("main organization");
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsTargetOrganizationKeyAfterValidateOpen() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        Organization targetOrg = Organization.builder().id(20L).tenantId(7L).name("Target").build();
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(20L).build();
        WorkflowOpenContext<OrganizationJoinRequestWorkflowPayload> ctx =
                new WorkflowOpenContext<>(1L, 7L, payload);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(targetOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(ctx);

        assertThat(handler.resolveOpenConcurrencyKeys(ctx, null))
                .containsExactly(WorkflowOpenConcurrencyKeys.organization(20L));
    }

    @Test
    void resolveContextOrganizationUsesTargetWhenUserHasNoMembership() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());

        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(20L).build();

        assertThat(handler.resolveContextOrganization(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isEqualTo(20L);
    }

    @Test
    void resolveSecurityContextOrganizationUsesEnabledOrgWhenUserHasNoMembership() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(workflowDefinitionRepository.findByWorkflowTypeId(
                        WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(
                        List.of(
                                WorkflowDefinition.builder()
                                        .enabled(false)
                                        .organizationId(10L)
                                        .build(),
                                WorkflowDefinition.builder()
                                        .enabled(true)
                                        .organizationId(20L)
                                        .build()));

        assertThat(handler.resolveSecurityContextOrganization(1L, 7L)).isEqualTo(20L);
    }

    @Test
    void validateOpenAllowsJoinWhenUserHasNoMembership() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization targetOrg =
                Organization.builder().id(20L).tenantId(7L).name("Target Fire Dept").build();
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(targetOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getTargetOrganizationName()).isEqualTo("Target Fire Dept");
    }

    @Test
    void validateOpenRejectsWhenPendingOnboardingExists() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        Organization targetOrg =
                Organization.builder().id(20L).tenantId(7L).name("Target Fire Dept").build();
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(targetOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);
        when(onboardingInvitationService.hasPendingForUserAndOrganization(1L, 20L))
                .thenReturn(true);

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("already have onboarding");
    }

    @Test
    void onApprovedOpensOnboardingWhenStepsRemain() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload(
                                "{\"targetOrganizationId\":20,\"targetOrganizationName\":\"Target\"}")
                        .build();
        when(onboardingInvitationService.recordInvitationForAdmission(7L, 1L, 20L, null))
                .thenReturn(false);

        handler.onApproved(workflow);

        verify(onboardingInvitationService).recordInvitationForAdmission(7L, 1L, 20L, null);
        verify(userService, never())
                .ensureOrganizationMembershipAndAllMembersGroup(anyLong(), anyLong(), anyLong());
    }

    @Test
    void onApprovedAdmitsDirectlyWhenAllStepsLiveFulfilled() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload(
                                "{\"targetOrganizationId\":20,\"targetOrganizationName\":\"Target\"}")
                        .build();
        when(onboardingInvitationService.recordInvitationForAdmission(7L, 1L, 20L, null))
                .thenReturn(true);

        handler.onApproved(workflow);

        verify(onboardingInvitationService).recordInvitationForAdmission(7L, 1L, 20L, null);
        verify(userService).ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 20L);
    }

    @Test
    void onOpenedAttachesDocuments() {
        Workflow workflow = Workflow.builder().id(3L).build();
        handler.onOpened(workflow);
        verify(workflowDocumentService)
                .attachOrganizationJoinRequestAttachmentsToWorkflow(workflow);
    }

    @Test
    void validateUpdateRejectsChangingTargetOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Workflow existing =
                Workflow.builder()
                        .id(4L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"targetOrganizationId\":20}")
                        .build();
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(21L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                handler.validateUpdate(
                                        new WorkflowOpenContext<>(1L, 7L, payload), existing))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("cannot be changed");
        verify(organizationRepository, never()).findById(anyLong());
    }

    @Test
    void resolveDocumentPromotionMetadataInterpolatesTargetOrganizationName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(7L)
                        .payload(
                                "{\"targetOrganizationId\":20,\"targetOrganizationName\":\"FF Musterstadt\"}")
                        .build();
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.organizationJoinRequest",
                        Map.of("name", "FF Musterstadt")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Eintritt FF Musterstadt")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Eintritt FF Musterstadt");
    }
}
