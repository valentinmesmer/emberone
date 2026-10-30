/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.mainorganizationchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.dto.MainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
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
class MainOrganizationChangeWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock UserOrganizationMembershipService membershipService;
    @Mock OrganizationRepository organizationRepository;
    @Mock WorkflowTypeMappingService workflowTypeMappingService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private MainOrganizationChangeWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new MainOrganizationChangeWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        membershipService,
                        organizationRepository,
                        workflowTypeMappingService,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesDynamicMainOrganizationChangeMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.MAIN_ORGANIZATION_CHANGE);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.DYNAMIC);
        assertThat(handler.payloadType()).isEqualTo(MainOrganizationChangeWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
    }

    @Test
    void resolveDynamicOrganizationsReturnsNewMainOrganization() {
        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder().newMainOrganizationId(25L).build();

        List<ApproverOrg> orgs =
                handler.resolveDynamicOrganizations(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(orgs).containsExactly(new ApproverOrg(25L));
    }

    @Test
    void validateOpenNormalizesPayloadAndChecksNewMainOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        Organization newMainOrg =
                Organization.builder().id(20L).tenantId(7L).name("Second Fire Dept").build();
        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder()
                        .newMainOrganizationId(20L)
                        .reason("  New main org  ")
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(newMainOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.MAIN_ORGANIZATION_CHANGE.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getNewMainOrganizationName()).isEqualTo("Second Fire Dept");
        assertThat(payload.getReason()).isEqualTo("New main org");
    }

    @Test
    void validateOpenRejectsNewMainEqualToCurrentMainOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder().newMainOrganizationId(10L).build();

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
                .hasMessageContaining("current main organization");
    }

    @Test
    void validateOpenRejectsWhenNotMemberOfNewMainOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        Organization newMainOrg =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder().newMainOrganizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(newMainOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("active member");
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsNewMainOrganizationKeyAfterValidateOpen() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization mainOrg = Organization.builder().id(10L).build();
        Organization newMainOrg =
                Organization.builder().id(20L).tenantId(7L).name("Target").build();
        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder().newMainOrganizationId(20L).build();
        WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> ctx =
                new WorkflowOpenContext<>(1L, 7L, payload);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(mainOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(newMainOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.MAIN_ORGANIZATION_CHANGE.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(ctx);

        assertThat(handler.resolveOpenConcurrencyKeys(ctx, null))
                .containsExactly(WorkflowOpenConcurrencyKeys.organization(20L));
    }

    @Test
    void resolveContextOrganizationUsesNewMainWhenUserHasNoMainOrganization() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());

        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder().newMainOrganizationId(20L).build();

        assertThat(handler.resolveContextOrganization(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isEqualTo(20L);
    }

    @Test
    void validateOpenAllowsChangeWhenUserHasNoMainOrganization() {
        User user = User.builder().id(1L).tenantId(7L).build();
        Organization newMainOrg =
                Organization.builder().id(20L).tenantId(7L).name("Second Fire Dept").build();
        MainOrganizationChangeWorkflowPayload payload =
                MainOrganizationChangeWorkflowPayload.builder().newMainOrganizationId(20L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(20L)).thenReturn(Optional.of(newMainOrg));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(true);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.MAIN_ORGANIZATION_CHANGE.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getNewMainOrganizationName()).isEqualTo("Second Fire Dept");
    }

    @Test
    void onApprovedChangesMainOrganization() {
        Workflow workflow =
                Workflow.builder()
                        .id(5L)
                        .openedByUserId(1L)
                        .payload(
                                "{\"newMainOrganizationId\":20,\"newMainOrganizationName\":\"Target\"}")
                        .build();

        handler.onApproved(workflow);

        verify(membershipService).changeMainOrganization(1L, 20L);
    }

    @Test
    void onOpenedAttachesDocuments() {
        Workflow workflow = Workflow.builder().id(3L).build();
        handler.onOpened(workflow);
        verify(workflowDocumentService).attachMainOrganizationChangeAttachmentsToWorkflow(workflow);
    }

    @Test
    void resolveDocumentPromotionMetadataInterpolatesNewMainOrganizationName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(7L)
                        .payload(
                                "{\"newMainOrganizationId\":20,\"newMainOrganizationName\":\"Second Fire Dept\"}")
                        .build();
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.mainOrganizationChange",
                        Map.of("name", "Second Fire Dept")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Hauptorganisation Second Fire Dept")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Hauptorganisation Second Fire Dept");
    }
}
