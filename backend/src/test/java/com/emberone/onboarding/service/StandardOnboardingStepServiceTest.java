/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.emergencycontact.repository.UserEmergencyContactRepository;
import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.handler.CertificationsStepHandler;
import com.emberone.onboarding.handler.DrivingLicenseStepHandler;
import com.emberone.onboarding.handler.EmergencyContactsStepHandler;
import com.emberone.onboarding.handler.EntryDocumentsStepHandler;
import com.emberone.onboarding.handler.TrainingCoursesStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.onboarding.standard.StandardOnboardingStepRepository;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardKind;
import com.emberone.standard.domain.StandardSource;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandardOnboardingStepServiceTest {

    @Mock StandardOnboardingStepRepository stepRepository;
    @Mock TenantOnboardingDocumentDefinitionRepository definitionRepository;
    @Mock UserEmergencyContactRepository emergencyContactRepository;
    @Mock OnboardingEntryDocumentReuseService documentReuseService;
    @Mock OnboardingEntryDocumentDraftSupport documentDraftSupport;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;
    @Mock OnboardingDocumentTemplateService documentTemplateService;
    @Mock OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    @Mock EmergencyContactService emergencyContactService;
    @Mock TrainingCourseRecordService trainingCourseRecordService;
    @Mock CertificationAssignmentService certificationAssignmentService;
    @Mock CertificationRecordService certificationRecordService;
    @Mock DrivingLicenseRecordService drivingLicenseRecordService;

    private StandardOnboardingStepService service;

    @BeforeEach
    void setUp() {
        OnboardingStepHandlerRegistry registry =
                new OnboardingStepHandlerRegistry(
                        List.of(
                                new EntryDocumentsStepHandler(
                                        definitionRepository,
                                        documentTemplateService,
                                        documentReuseService,
                                        documentDraftSupport,
                                        attachmentPromotionService),
                                new EmergencyContactsStepHandler(
                                        emergencyContactRepository, emergencyContactService),
                                new TrainingCoursesStepHandler(
                                        mainOrganizationSkipEvaluator,
                                        trainingCourseRecordService,
                                        attachmentPromotionService),
                                new CertificationsStepHandler(
                                        mainOrganizationSkipEvaluator,
                                        certificationAssignmentService,
                                        certificationRecordService,
                                        attachmentPromotionService),
                                new DrivingLicenseStepHandler(
                                        mainOrganizationSkipEvaluator,
                                        drivingLicenseRecordService,
                                        attachmentPromotionService)));
        service = new StandardOnboardingStepService(stepRepository, registry);
    }

    @Test
    void replaceStepsRejectsDuplicateTypes() {
        Standard standard = standard(1L, 10L);

        assertThatThrownBy(
                        () ->
                                service.replaceSteps(
                                        standard,
                                        List.of(
                                                new StandardOnboardingStepService.StepReplacement(
                                                        OnboardingStepType.EMERGENCY_CONTACTS,
                                                        0,
                                                        Map.of("minimumCount", 1)),
                                                new StandardOnboardingStepService.StepReplacement(
                                                        OnboardingStepType.EMERGENCY_CONTACTS,
                                                        1,
                                                        Map.of("minimumCount", 2)))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void replaceStepsPersistsEmergencyContactsStep() {
        Standard standard = standard(5L, 10L);
        when(stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(5L)).thenReturn(List.of());
        when(stepRepository.save(any(StandardOnboardingStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<StandardOnboardingStep> saved =
                service.replaceSteps(
                        standard,
                        List.of(
                                new StandardOnboardingStepService.StepReplacement(
                                        OnboardingStepType.EMERGENCY_CONTACTS,
                                        0,
                                        Map.of("minimumCount", 2))));

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStepType()).isEqualTo(OnboardingStepType.EMERGENCY_CONTACTS);
        assertThat(saved.getFirst().getConfig()).containsEntry("minimumCount", 2);
    }

    @Test
    void replaceEntryDocumentsStepWithEmptyKeysRemovesEntryDocumentsStep() {
        Standard standard = standard(5L, 10L);
        StandardOnboardingStep entryStep =
                StandardOnboardingStep.builder()
                        .tenantId(10L)
                        .standardId(5L)
                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                        .sortOrder(0)
                        .config(Map.of("documentConfigKeys", List.of("privacy")))
                        .build();
        StandardOnboardingStep contactsStep =
                StandardOnboardingStep.builder()
                        .tenantId(10L)
                        .standardId(5L)
                        .stepType(OnboardingStepType.EMERGENCY_CONTACTS)
                        .sortOrder(1)
                        .config(Map.of("minimumCount", 1))
                        .build();
        when(stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(5L))
                .thenReturn(List.of(entryStep, contactsStep));
        when(stepRepository.save(any(StandardOnboardingStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceEntryDocumentsStep(standard, List.of());

        ArgumentCaptor<StandardOnboardingStep> captor =
                ArgumentCaptor.forClass(StandardOnboardingStep.class);
        verify(stepRepository).save(captor.capture());
        assertThat(captor.getValue().getStepType())
                .isEqualTo(OnboardingStepType.EMERGENCY_CONTACTS);
    }

    @Test
    void copyStepsClonesFromSource() {
        Standard source = standard(1L, 10L);
        Standard target = standard(2L, 10L);
        StandardOnboardingStep sourceStep =
                StandardOnboardingStep.builder()
                        .tenantId(10L)
                        .standardId(1L)
                        .stepType(OnboardingStepType.TRAINING_COURSES)
                        .sortOrder(0)
                        .config(Map.of())
                        .build();
        when(stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(sourceStep));
        when(stepRepository.save(any(StandardOnboardingStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.copySteps(source, target);

        ArgumentCaptor<StandardOnboardingStep> captor =
                ArgumentCaptor.forClass(StandardOnboardingStep.class);
        verify(stepRepository).deleteByStandardId(2L);
        verify(stepRepository).save(captor.capture());
        assertThat(captor.getValue().getStandardId()).isEqualTo(2L);
        assertThat(captor.getValue().getStepType()).isEqualTo(OnboardingStepType.TRAINING_COURSES);
    }

    private static Standard standard(long id, long tenantId) {
        return Standard.builder()
                .id(id)
                .tenantId(tenantId)
                .kind(StandardKind.ORGANIZATION_SETUP)
                .source(StandardSource.CUSTOM)
                .name("Test")
                .build();
    }
}
