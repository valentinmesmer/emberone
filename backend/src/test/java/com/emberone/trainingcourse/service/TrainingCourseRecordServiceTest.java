/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionGrantedType;
import com.emberone.trainingcourse.dto.CreateTrainingCourseUserRecordRequest;
import com.emberone.trainingcourse.dto.UpdateTrainingCourseUserRecordRequest;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionDocumentRepository;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.userdocument.service.UserDocumentSystemWriter;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TrainingCourseRecordServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;
    private static final Long ORG_ID = 10L;
    private static final Long TYPE_ID = 7L;

    @Mock TrainingCourseUserSubmissionRepository submissionRepository;
    @Mock TrainingCourseUserSubmissionDocumentRepository submissionDocumentRepository;
    @Mock TenantTrainingCourseTypeRepository tenantTypeRepository;
    @Mock UserDocumentRepository userDocumentRepository;
    @Mock UserDocumentService userDocumentService;
    @Mock TokenService tokenService;
    @Mock TrainingCourseCatalogService catalogService;
    @Mock TenantLocaleService tenantLocaleService;
    @Mock LocalizationService localizationService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock JsonMapper jsonMapper;
    @Mock WorkflowRepository workflowRepository;
    @Mock com.emberone.platform.navigation.AppNavigationResolver appNavigationResolver;

    TrainingCourseRecordService service;

    @BeforeEach
    void setUp() {
        LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport =
                new LocalizedWorkflowDocumentPromotionSupport(
                        tenantLocaleService, localizationService, new UserDocumentSystemWriter());
        service =
                new TrainingCourseRecordService(
                        submissionRepository,
                        submissionDocumentRepository,
                        tenantTypeRepository,
                        userDocumentRepository,
                        userDocumentService,
                        tokenService,
                        catalogService,
                        documentPromotionMetadataSupport,
                        workflowRepository,
                        jsonMapper,
                        eventPublisher,
                        appNavigationResolver);
    }

    @Test
    void createManualRejectsInvalidDateRange() {
        CreateTrainingCourseUserRecordRequest request = new CreateTrainingCourseUserRecordRequest();
        request.setTenantTrainingCourseTypeIds(List.of(TYPE_ID));
        request.setStartDate(LocalDate.of(2026, 7, 10));
        request.setEndDate(LocalDate.of(2026, 7, 2));

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);

        assertThatThrownBy(() -> service.createManual(ORG_ID, USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("startDate must be on or before endDate");
    }

    @Test
    void createManualRejectsWhenTypeNotAvailableForOrg() {
        CreateTrainingCourseUserRecordRequest request = new CreateTrainingCourseUserRecordRequest();
        request.setTenantTrainingCourseTypeIds(List.of(TYPE_ID));
        request.setStartDate(LocalDate.of(2026, 7, 1));
        request.setEndDate(LocalDate.of(2026, 7, 2));
        request.setLocation("Landesfeuerwehrschule");

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        doThrow(
                        new ConflictException(
                                "Training course type is not available for organization " + ORG_ID))
                .when(catalogService)
                .requireTypeAvailableForOrg(TYPE_ID, ORG_ID);

        assertThatThrownBy(() -> service.createManual(ORG_ID, USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available for organization");
    }

    @Test
    void createManualRejectsMultipleTypesWithoutName() {
        CreateTrainingCourseUserRecordRequest request = new CreateTrainingCourseUserRecordRequest();
        request.setTenantTrainingCourseTypeIds(List.of(TYPE_ID, 8L));
        request.setStartDate(LocalDate.of(2026, 7, 1));
        request.setEndDate(LocalDate.of(2026, 7, 2));
        request.setLocation("Landesfeuerwehrschule");

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        stubType(TYPE_ID, "Atemschutzlehrgang");
        stubType(8L, "Truppführer");

        assertThatThrownBy(() -> service.createManual(ORG_ID, USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("name is required when multiple types are granted");
    }

    @Test
    void createManualAppliesMetadataToLinkedDocument() {
        TenantTrainingCourseType type =
                TenantTrainingCourseType.builder()
                        .id(TYPE_ID)
                        .tenantId(TENANT_ID)
                        .name("Atemschutzlehrgang")
                        .active(true)
                        .archived(false)
                        .build();
        UserDocument document =
                UserDocument.builder()
                        .id(100L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(123L)
                        .storageKey("k")
                        .build();
        CreateTrainingCourseUserRecordRequest request = new CreateTrainingCourseUserRecordRequest();
        request.setTenantTrainingCourseTypeIds(List.of(TYPE_ID));
        request.setStartDate(LocalDate.of(2026, 7, 1));
        request.setEndDate(LocalDate.of(2026, 7, 2));
        request.setLocation("Landesfeuerwehrschule");
        request.setHours(new BigDecimal("8.00"));
        request.setUserDocumentIds(List.of(100L));

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        when(tenantTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type));
        when(tenantTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type));
        when(tenantLocaleService.resolveTenantLocale(TENANT_ID)).thenReturn(SupportedLocale.DE);
        when(submissionRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            TrainingCourseUserSubmission submission = invocation.getArgument(0);
                            submission.setId(55L);
                            submission
                                    .getGrantedTypes()
                                    .forEach(grantedType -> grantedType.setId(1L));
                            return submission;
                        });
        when(userDocumentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(document));
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.trainingCourseRecord.title",
                        SupportedLocale.DE))
                .thenReturn("Nachweis {name}");
        when(localizationService.getInterpolatedMessage(
                        "workflow.documentPromotion.trainingCourseRecord.title",
                        Map.of("name", "Atemschutzlehrgang"),
                        SupportedLocale.DE))
                .thenReturn("Nachweis Atemschutzlehrgang");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.trainingCourseRecord.description",
                        SupportedLocale.DE))
                .thenReturn("Nachweis aus einem eingereichten Lehrgang");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.trainingCourseRecord.tag", SupportedLocale.DE))
                .thenReturn("Lehrgang");

        service.createManual(ORG_ID, USER_ID, request);

        assertThat(document.getTitle()).isEqualTo("Nachweis Atemschutzlehrgang");
        assertThat(document.getFileName()).isEqualTo("Nachweis Atemschutzlehrgang.pdf");
        assertThat(document.getDescription())
                .isEqualTo("Nachweis aus einem eingereichten Lehrgang");
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags().get(0).getTag()).isEqualTo("Lehrgang");
        assertThat(document.getLinks()).hasSize(1);
        assertThat(document.getLinks().get(0).getLinkType())
                .isEqualTo(AppLinkType.TRAINING_COURSE_RECORD);
        assertThat(document.getLinks().get(0).getLinkId()).isEqualTo(55L);
    }

    @Test
    void createSubmissionFromOnboardingAppliesMetadataToLinkedDocument() {
        TenantTrainingCourseType type =
                TenantTrainingCourseType.builder()
                        .id(TYPE_ID)
                        .tenantId(TENANT_ID)
                        .name("Atemschutzlehrgang")
                        .active(true)
                        .archived(false)
                        .build();
        UserDocument document =
                UserDocument.builder()
                        .id(100L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(123L)
                        .storageKey("k")
                        .build();

        when(tenantTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type));
        when(tenantLocaleService.resolveTenantLocale(TENANT_ID)).thenReturn(SupportedLocale.DE);
        when(submissionRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            TrainingCourseUserSubmission submission = invocation.getArgument(0);
                            submission.setId(55L);
                            submission
                                    .getGrantedTypes()
                                    .forEach(grantedType -> grantedType.setId(1L));
                            return submission;
                        });
        when(userDocumentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(document));
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.trainingCourseRecord.title",
                        SupportedLocale.DE))
                .thenReturn("Lehrgang {name}");
        when(localizationService.getInterpolatedMessage(
                        "workflow.documentPromotion.trainingCourseRecord.title",
                        Map.of("name", "Atemschutzlehrgang"),
                        SupportedLocale.DE))
                .thenReturn("Lehrgang Atemschutzlehrgang");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.trainingCourseRecord.description",
                        SupportedLocale.DE))
                .thenReturn("Nachweis aus einem eingereichten Lehrgang");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.trainingCourseRecord.tag", SupportedLocale.DE))
                .thenReturn("Lehrgang");

        service.createSubmissionFromOnboarding(
                TENANT_ID,
                USER_ID,
                ORG_ID,
                "dsfds",
                List.of(TYPE_ID),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                "Landesfeuerwehrschule",
                new BigDecimal("8.00"),
                null,
                9L,
                100L);

        assertThat(document.getTitle()).isEqualTo("Lehrgang Atemschutzlehrgang");
        assertThat(document.getFileName()).isEqualTo("Lehrgang Atemschutzlehrgang.pdf");
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags().get(0).getTag()).isEqualTo("Lehrgang");
    }

    @Test
    void updateManualUpdatesSubmissionFields() {
        TenantTrainingCourseType type =
                TenantTrainingCourseType.builder()
                        .id(TYPE_ID)
                        .tenantId(TENANT_ID)
                        .name("Gruppenführer")
                        .active(true)
                        .archived(false)
                        .build();
        TrainingCourseUserSubmission submission =
                TrainingCourseUserSubmission.builder()
                        .id(55L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .name("Gruppenführer")
                        .startDate(LocalDate.of(2026, 4, 13))
                        .endDate(LocalDate.of(2026, 5, 21))
                        .location("Alte Schule")
                        .build();
        submission
                .getGrantedTypes()
                .add(
                        TrainingCourseUserSubmissionGrantedType.builder()
                                .tenantId(TENANT_ID)
                                .submission(submission)
                                .tenantTrainingCourseTypeId(TYPE_ID)
                                .build());
        UpdateTrainingCourseUserRecordRequest request = new UpdateTrainingCourseUserRecordRequest();
        request.setStartDate(LocalDate.of(2026, 4, 14));
        request.setEndDate(LocalDate.of(2026, 5, 22));
        request.setLocation("Landesfeuerwehrschule");
        request.setHours(new BigDecimal("40.00"));
        request.setNotes("Aktualisiert");

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        when(submissionRepository.findById(55L)).thenReturn(Optional.of(submission));
        when(tenantTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type));
        when(tenantTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(submissionDocumentRepository.findBySubmission_Id(55L)).thenReturn(List.of());

        var result = service.updateManual(ORG_ID, USER_ID, 55L, request);

        assertThat(submission.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 14));
        assertThat(submission.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(submission.getLocation()).isEqualTo("Landesfeuerwehrschule");
        assertThat(submission.getHours()).isEqualByComparingTo("40.00");
        assertThat(submission.getNotes()).isEqualTo("Aktualisiert");
        assertThat(result.getLocation()).isEqualTo("Landesfeuerwehrschule");
    }

    @Test
    void deleteManualRemovesSubmission() {
        TrainingCourseUserSubmission submission =
                TrainingCourseUserSubmission.builder()
                        .id(55L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .name("Gruppenführer")
                        .startDate(LocalDate.of(2026, 4, 13))
                        .endDate(LocalDate.of(2026, 5, 21))
                        .build();
        submission
                .getGrantedTypes()
                .add(
                        TrainingCourseUserSubmissionGrantedType.builder()
                                .tenantId(TENANT_ID)
                                .submission(submission)
                                .tenantTrainingCourseTypeId(TYPE_ID)
                                .build());

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        when(submissionRepository.findById(55L)).thenReturn(Optional.of(submission));

        service.deleteManual(ORG_ID, USER_ID, 55L);

        verify(userDocumentService)
                .deleteDocumentsForLinkTargets(
                        TENANT_ID, AppLinkType.TRAINING_COURSE_RECORD, List.of(55L));
        verify(submissionRepository).delete(submission);
    }

    private void stubType(Long typeId, String name) {
        when(tenantTypeRepository.findById(typeId))
                .thenReturn(
                        Optional.of(
                                TenantTrainingCourseType.builder()
                                        .id(typeId)
                                        .tenantId(TENANT_ID)
                                        .name(name)
                                        .active(true)
                                        .build()));
    }
}
