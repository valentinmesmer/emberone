/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.trainingcourse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.exception.ConflictException;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowDocumentService;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class TrainingCourseRecordWorkflowHandlerTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 7L;
    private static final long MEMBERSHIP_ORG_ID = 10L;
    private static final long TYPE_ID = 100L;
    private static final long TYPE_ID_2 = 101L;
    private static final String TEST_LOCATION = "Landesfeuerwehrschule";
    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock WorkflowDocumentService workflowDocumentService;
    @Mock TrainingCourseRecordService trainingCourseRecordService;
    @Mock TrainingCourseCatalogService catalogService;
    @Mock TenantTrainingCourseTypeRepository tenantTypeRepository;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;
    private TrainingCourseRecordWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new TrainingCourseRecordWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        workflowDocumentService,
                        trainingCourseRecordService,
                        catalogService,
                        tenantTypeRepository,
                        JsonMapper.builder().build());
    }

    @Test
    void allowsMultipleConcurrentOpen() {
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
    }

    @Test
    void validateOpenRejectsInvalidDateRange() {
        TrainingCourseRecordWorkflowPayload payload =
                TrainingCourseRecordWorkflowPayload.builder()
                        .tenantTrainingCourseTypeIds(List.of(TYPE_ID))
                        .startDate(LocalDate.of(2026, 7, 10))
                        .endDate(LocalDate.of(2026, 7, 2))
                        .location(TEST_LOCATION)
                        .attachmentIds(List.of(30L))
                        .build();

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("startDate must be on or before endDate");
    }

    @Test
    void validateOpenRejectsTypeNotAvailableForOrg() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        TrainingCourseRecordWorkflowPayload payload =
                TrainingCourseRecordWorkflowPayload.builder()
                        .tenantTrainingCourseTypeIds(List.of(TYPE_ID))
                        .startDate(LocalDate.of(2026, 7, 1))
                        .endDate(LocalDate.of(2026, 7, 2))
                        .location(TEST_LOCATION)
                        .attachmentIds(List.of(30L))
                        .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubDefinition(TYPE_ID);
        stubMembership(MEMBERSHIP_ORG_ID);
        doThrow(new ConflictException("Training course type is not available"))
                .when(catalogService)
                .requireTypeAvailableForOrg(TYPE_ID, MEMBERSHIP_ORG_ID);

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void validateOpenChecksAttachmentsAgainstMembershipOrganization() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        Organization membershipOrg = Organization.builder().id(MEMBERSHIP_ORG_ID).build();
        TrainingCourseRecordWorkflowPayload payload =
                TrainingCourseRecordWorkflowPayload.builder()
                        .tenantTrainingCourseTypeIds(List.of(TYPE_ID, TYPE_ID_2))
                        .name("Grundausbildung BF")
                        .startDate(LocalDate.of(2026, 7, 1))
                        .endDate(LocalDate.of(2026, 7, 2))
                        .location(TEST_LOCATION)
                        .hours(new BigDecimal("8.00"))
                        .attachmentIds(List.of(30L))
                        .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubDefinition(TYPE_ID);
        stubDefinition(TYPE_ID_2);
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
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsTrainingCourseTypeKeysAfterValidateOpen() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        TrainingCourseRecordWorkflowPayload payload =
                TrainingCourseRecordWorkflowPayload.builder()
                        .tenantTrainingCourseTypeIds(List.of(TYPE_ID, TYPE_ID_2))
                        .name("Grundausbildung BF")
                        .startDate(LocalDate.of(2026, 7, 1))
                        .endDate(LocalDate.of(2026, 7, 2))
                        .location(TEST_LOCATION)
                        .attachmentIds(List.of(30L))
                        .build();
        WorkflowOpenContext<TrainingCourseRecordWorkflowPayload> ctx =
                new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubDefinition(TYPE_ID);
        stubDefinition(TYPE_ID_2);
        stubMembership(MEMBERSHIP_ORG_ID);
        when(attachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                        30L, EntityAttachmentType.WORKFLOW_DRAFT, USER_ID, MEMBERSHIP_ORG_ID))
                .thenReturn(Optional.of(EntityAttachment.builder().uploadedBy(USER_ID).build()));

        handler.validateOpen(ctx);

        assertThat(handler.resolveOpenConcurrencyKeys(ctx, null))
                .containsExactly(
                        WorkflowOpenConcurrencyKeys.trainingCourseType(TYPE_ID),
                        WorkflowOpenConcurrencyKeys.trainingCourseType(TYPE_ID_2));
    }

    @Test
    void validateOpenRejectsMultipleTypesWithoutName() {
        TrainingCourseRecordWorkflowPayload payload =
                TrainingCourseRecordWorkflowPayload.builder()
                        .tenantTrainingCourseTypeIds(List.of(TYPE_ID, TYPE_ID_2))
                        .startDate(LocalDate.of(2026, 7, 1))
                        .endDate(LocalDate.of(2026, 7, 2))
                        .location(TEST_LOCATION)
                        .attachmentIds(List.of(30L))
                        .build();

        assertThatThrownBy(
                        () ->
                                handler.validateOpen(
                                        new WorkflowOpenContext<>(USER_ID, TENANT_ID, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void resolveDocumentPromotionMetadataUsesSubmissionName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(TENANT_ID)
                        .payload(
                                "{\"tenantTrainingCourseTypeIds\":["
                                        + TYPE_ID
                                        + ","
                                        + TYPE_ID_2
                                        + "],\"name\":\"Grundausbildung BF\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\"}")
                        .build();
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.trainingCourseRecord",
                        Map.of("name", "Grundausbildung BF")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Nachweis Grundausbildung BF")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Nachweis Grundausbildung BF");
    }

    @Test
    void resolveDocumentPromotionMetadataUsesCombinedTypeNamesWhenNoCustomName() {
        Workflow workflow =
                Workflow.builder()
                        .tenantId(TENANT_ID)
                        .payload(
                                "{\"tenantTrainingCourseTypeIds\":["
                                        + TYPE_ID
                                        + ","
                                        + TYPE_ID_2
                                        + "],\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-02\"}")
                        .build();
        when(tenantTypeRepository.findById(TYPE_ID))
                .thenReturn(
                        Optional.of(
                                TenantTrainingCourseType.builder()
                                        .name("Grundausbildung")
                                        .build()));
        when(tenantTypeRepository.findById(TYPE_ID_2))
                .thenReturn(
                        Optional.of(
                                TenantTrainingCourseType.builder().name("Truppführer").build()));
        when(documentPromotionMetadataSupport.fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.trainingCourseRecord",
                        Map.of("name", "Grundausbildung, Truppführer")))
                .thenReturn(
                        Optional.of(
                                WorkflowDocumentPromotionMetadata.builder()
                                        .title("Nachweis Grundausbildung, Truppführer")
                                        .build()));

        assertThat(handler.resolveDocumentPromotionMetadata(workflow))
                .map(WorkflowDocumentPromotionMetadata::getTitle)
                .contains("Nachweis Grundausbildung, Truppführer");
        verify(documentPromotionMetadataSupport)
                .fromInterpolatedTitle(
                        workflow,
                        "workflow.documentPromotion.trainingCourseRecord",
                        Map.of("name", "Grundausbildung, Truppführer"));
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

    private void stubDefinition(long typeId) {
        when(tenantTypeRepository.findById(typeId))
                .thenReturn(
                        Optional.of(
                                TenantTrainingCourseType.builder()
                                        .id(typeId)
                                        .tenantId(TENANT_ID)
                                        .active(true)
                                        .build()));
    }
}
