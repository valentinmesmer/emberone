/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.certificationrecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.certification.domain.CertEvidenceType;
import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationCatalogService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.certification.service.TenantCertDefinitionEvidenceTypeService;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.CertificationRecordWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CertificationRecordWorkflowHandlerTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 7L;
    private static final long MEMBERSHIP_ORG_ID = 10L;
    private static final long DEFINITION_ID = 100L;

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock CertificationRecordService certificationRecordService;
    @Mock CertificationAssignmentService assignmentService;
    @Mock CertificationCatalogService catalogService;
    @Mock TenantCertDefinitionRepository tenantDefinitionRepository;
    @Mock TenantCertDefinitionEvidenceTypeService evidenceTypeService;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private CertificationRecordWorkflowHandler handler;

    private static CertificationRecordWorkflowPayload payload(Long definitionId) {
        return CertificationRecordWorkflowPayload.builder()
                .tenantCertDefinitionId(definitionId)
                .completedDate(LocalDate.now())
                .attachmentIds(List.of(30L))
                .build();
    }

    @BeforeEach
    void setUp() {
        lenient()
                .when(certificationRecordService.resolveExpiryForCapture(any(), any(), isNull()))
                .thenReturn(LocalDate.now().plusYears(1));
        handler =
                new CertificationRecordWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        certificationRecordService,
                        assignmentService,
                        catalogService,
                        tenantDefinitionRepository,
                        evidenceTypeService,
                        JsonMapper.builder().build());
    }

    @Test
    void allowsMultipleConcurrentOpen() {
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
    }

    @Test
    void resolveSecurityContextOrganizationUsesMembershipWithoutPayload() {
        stubMembership(MEMBERSHIP_ORG_ID);

        Long orgId = handler.resolveSecurityContextOrganization(USER_ID, TENANT_ID);

        assertThat(orgId).isEqualTo(MEMBERSHIP_ORG_ID);
    }

    @Test
    void resolveContextOrganizationUsesMembershipOrganization() {
        stubMembership(MEMBERSHIP_ORG_ID);
        stubDefinition(DEFINITION_ID);
        CertificationRecordWorkflowPayload payload = payload(DEFINITION_ID);

        Long orgId =
                handler.resolveContextOrganization(
                        new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload));

        assertThat(orgId).isEqualTo(MEMBERSHIP_ORG_ID);
    }

    @Test
    void validateOpenChecksAttachmentsAgainstMembershipOrganization() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        Organization membershipOrg = Organization.builder().id(MEMBERSHIP_ORG_ID).build();
        CertificationRecordWorkflowPayload payload = payload(DEFINITION_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubDefinition(DEFINITION_ID);
        stubEvidenceTypes(DEFINITION_ID, Set.of(CertEvidenceType.DOCUMENT));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(membershipOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        30L, EntityAttachmentType.WORKFLOW_DRAFT, USER_ID, MEMBERSHIP_ORG_ID))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(USER_ID).build()));

        handler.validateOpen(new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload));

        verify(catalogService).requireTypeAvailableForOrg(DEFINITION_ID, MEMBERSHIP_ORG_ID);
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsDefinitionKey() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        CertificationRecordWorkflowPayload payload = payload(DEFINITION_ID);
        WorkflowOpenContext<CertificationRecordWorkflowPayload> ctx =
                new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubMembership(MEMBERSHIP_ORG_ID);
        stubDefinition(DEFINITION_ID);
        stubEvidenceTypes(DEFINITION_ID, Set.of(CertEvidenceType.DOCUMENT));
        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        30L, EntityAttachmentType.WORKFLOW_DRAFT, USER_ID, MEMBERSHIP_ORG_ID))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(USER_ID).build()));

        handler.validateOpen(ctx);

        assertThat(handler.resolveOpenConcurrencyKeys(ctx, null))
                .containsExactly(
                        WorkflowOpenConcurrencyKeys.certificationDefinition(DEFINITION_ID));
    }

    @Test
    void validateOpenAcceptsUnassignedCatalogDefinition() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        Organization membershipOrg = Organization.builder().id(MEMBERSHIP_ORG_ID).build();
        CertificationRecordWorkflowPayload payload = payload(DEFINITION_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubDefinition(DEFINITION_ID);
        stubEvidenceTypes(DEFINITION_ID, Set.of(CertEvidenceType.DOCUMENT));
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(membershipOrg)
                                        .startDate(Instant.now())
                                        .build()));
        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        30L, EntityAttachmentType.WORKFLOW_DRAFT, USER_ID, MEMBERSHIP_ORG_ID))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(USER_ID).build()));

        handler.validateOpen(new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload));

        verify(catalogService).requireTypeAvailableForOrg(DEFINITION_ID, MEMBERSHIP_ORG_ID);
    }

    @Test
    void resolveContextOrganizationRejectsMissingPayload() {
        assertThatThrownBy(
                        () ->
                                handler.resolveContextOrganization(
                                        new WorkflowOpenContext<>(USER_ID, TENANT_ID, null)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Payload is required");
    }

    @Test
    void onApprovedDoesNotDeleteProofAttachmentsBeforePromotion() {
        LocalDate completedDate = LocalDate.of(2026, 1, 15);
        Workflow workflow =
                Workflow.builder()
                        .id(50L)
                        .tenantId(TENANT_ID)
                        .openedByUserId(USER_ID)
                        .workflowTypeId(WorkflowType.CERTIFICATION_RECORD.getId())
                        .payload(
                                "{\"tenantCertDefinitionId\":"
                                        + DEFINITION_ID
                                        + ",\"completedDate\":\"2026-01-15\",\"notes\":\"ok\"}")
                        .build();

        handler.onApproved(workflow);

        verify(workflowDocumentService, never()).deleteCertificationProofAttachments(any());
        verify(certificationRecordService)
                .createFromWorkflow(
                        TENANT_ID, USER_ID, List.of(DEFINITION_ID), completedDate, "ok", 50L);
    }

    @Test
    void allowsPromotionAndDeletesProcessFilesAfterLink() {
        Workflow workflow = certificationWorkflow();

        assertThat(handler.allowsDocumentPromotion(workflow)).isTrue();

        handler.onDocumentsPromoted(workflow, List.of(901L));

        verify(certificationRecordService).linkDocumentsFromWorkflow(TENANT_ID, 50L, List.of(901L));
        verify(workflowDocumentService).deleteCertificationProofAttachments(workflow);
    }

    @Test
    void deletesProcessFilesOnRejectAndCancelWithoutPromotion() {
        Workflow workflow = certificationWorkflow();

        handler.onRejected(workflow);
        handler.onCancelled(workflow);

        verify(workflowDocumentService, times(2)).deleteCertificationProofAttachments(workflow);
    }

    @Test
    void onDocumentsPromotedDeletesProcessFilesWhenDocumentsWerePromoted() {
        Workflow workflow = certificationWorkflow();

        handler.onDocumentsPromoted(workflow, List.of(902L));

        verify(workflowDocumentService).deleteCertificationProofAttachments(workflow);
    }

    private static Workflow certificationWorkflow() {
        return Workflow.builder()
                .id(50L)
                .tenantId(TENANT_ID)
                .openedByUserId(USER_ID)
                .workflowTypeId(WorkflowType.CERTIFICATION_RECORD.getId())
                .payload(
                        "{\"tenantCertDefinitionId\":"
                                + DEFINITION_ID
                                + ",\"completedDate\":\"2026-01-15\"}")
                .build();
    }

    @Test
    void onApprovedCreatesAssignmentsThenRecord() {
        LocalDate completedDate = LocalDate.of(2026, 1, 15);
        Workflow workflow =
                Workflow.builder()
                        .id(50L)
                        .tenantId(TENANT_ID)
                        .openedByUserId(USER_ID)
                        .workflowTypeId(WorkflowType.CERTIFICATION_RECORD.getId())
                        .payload(
                                "{\"tenantCertDefinitionId\":"
                                        + DEFINITION_ID
                                        + ",\"completedDate\":\"2026-01-15\",\"notes\":\"ok\"}")
                        .build();

        handler.onApproved(workflow);

        verify(assignmentService)
                .createAssignmentsFromWorkflow(TENANT_ID, USER_ID, 50L, List.of(DEFINITION_ID));
        verify(certificationRecordService)
                .createFromWorkflow(
                        TENANT_ID, USER_ID, List.of(DEFINITION_ID), completedDate, "ok", 50L);
    }

    @Test
    void resolveDocumentPromotionMetadataUsesDefinitionName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(TENANT_ID)
                        .payload(
                                "{\"tenantCertDefinitionId\":"
                                        + DEFINITION_ID
                                        + ",\"completedDate\":\"2026-01-15\"}")
                        .build();
        when(tenantDefinitionRepository.findById(DEFINITION_ID))
                .thenReturn(
                        Optional.of(
                                TenantCertDefinition.builder()
                                        .name("Führerschein-Kontrolle C")
                                        .configKey("FSK C")
                                        .build()));
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.certificationRecord",
                        Map.of("name", "Führerschein-Kontrolle C")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Nachweis Führerschein-Kontrolle C")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Nachweis Führerschein-Kontrolle C");
        verify(documentPromotionMetadataSupport)
                .fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.certificationRecord",
                        Map.of("name", "Führerschein-Kontrolle C"));
    }

    private void stubMembership(long organizationId) {
        Organization organization = Organization.builder().id(organizationId).build();
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .startDate(Instant.now())
                                        .build()));
    }

    private void stubDefinition(long definitionId) {
        when(tenantDefinitionRepository.findById(definitionId))
                .thenReturn(
                        Optional.of(
                                TenantCertDefinition.builder()
                                        .id(definitionId)
                                        .tenantId(TENANT_ID)
                                        .active(true)
                                        .build()));
    }

    private void stubEvidenceTypes(Long definitionId, Set<CertEvidenceType> evidenceTypes) {
        when(evidenceTypeService.getEvidenceTypes(definitionId)).thenReturn(evidenceTypes);
    }
}
