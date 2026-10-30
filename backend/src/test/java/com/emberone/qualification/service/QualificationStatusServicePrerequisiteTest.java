/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.domain.UserDrivingLicense;
import com.emberone.drivinglicense.domain.UserDrivingLicenseClass;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.qualification.domain.QualificationExerciseStatus;
import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.tenant.*;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionGrantedType;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QualificationStatusServicePrerequisiteTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final long ORG_ID = 10L;
    private static final long DEFINITION_ID = 100L;
    private static final long TC_TYPE_A = 201L;
    private static final long TC_TYPE_B = 202L;
    private static final long CERT_A = 301L;
    private static final long CERT_B = 302L;
    private static final long DL_CLASS_C = 401L;

    @Mock QualificationCatalogService catalogService;
    @Mock TenantQualificationDefinitionRepository definitionRepository;
    @Mock UserQualificationOrgDecisionRepository orgDecisionRepository;
    @Mock CertificationStatusService certificationStatusService;
    @Mock TrainingCourseUserSubmissionRepository submissionRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    @Mock
    com.emberone.drivinglicense.repository.UserDrivingLicenseRepository drivingLicenseRepository;

    @Mock
    com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository
            drivingLicenseDefinitionRepository;

    @Mock TenantCertDefinitionRepository certDefinitionRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;

    private QualificationStatusService statusService;

    private static CertificationStatusItemDto certStatus(long definitionId, String status) {
        return CertificationStatusItemDto.builder()
                .tenantCertDefinitionId(definitionId)
                .status(status)
                .build();
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        statusService =
                new QualificationStatusService(
                        catalogService,
                        definitionRepository,
                        orgDecisionRepository,
                        new PrerequisiteEvaluationService(
                                certificationStatusService,
                                submissionRepository,
                                trainingCourseTypeRepository,
                                drivingLicenseRepository,
                                drivingLicenseDefinitionRepository,
                                certDefinitionRepository),
                        membershipRepository);
        when(certificationStatusService.getStatusForUser(USER_ID))
                .thenReturn(UserCertificationStatusDto.builder().items(List.of()).build());
        when(orgDecisionRepository
                        .findByUserIdAndTenantQualificationDefinitionIdAndOrganizationIdOrderByStartDateDesc(
                                anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void trainingCourses_allMode_requiresEveryConfiguredType() {
        TenantQualificationDefinition definition =
                definitionWithTrainingCourses(QualificationRequirementMode.ALL, "TC_A", "TC_B");
        stubTrainingCourseTypes();
        stubGrantedTrainingCourses(TC_TYPE_A);

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.PREREQUISITES_MISSING);
    }

    @Test
    void trainingCourses_anyMode_requiresAtLeastOneConfiguredType() {
        TenantQualificationDefinition definition =
                definitionWithTrainingCourses(QualificationRequirementMode.ANY, "TC_A", "TC_B");
        stubTrainingCourseTypes();
        stubGrantedTrainingCourses(TC_TYPE_B);

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.CAN_EXERCISE);
    }

    @Test
    void certDefinitions_allMode_countsOnlyValidCertifications() {
        TenantQualificationDefinition definition =
                definitionWithCertifications(QualificationRequirementMode.ALL, "CERT_A", "CERT_B");
        stubCertDefinitions();
        when(certificationStatusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .items(
                                        List.of(
                                                certStatus(CERT_A, "VALID"),
                                                certStatus(CERT_B, "EXPIRED")))
                                .build());

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.PREREQUISITES_MISSING);
    }

    @Test
    void drivingLicenses_expiredSubmission_doesNotSatisfyPrerequisite() {
        TenantQualificationDefinition definition =
                definitionWithDrivingLicenses(QualificationRequirementMode.ALL, "DL_C");
        stubDrivingLicenseDefinitions();
        stubGrantedDrivingLicenses(LocalDate.now().minusDays(1), null, DL_CLASS_C);

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.PREREQUISITES_MISSING);
    }

    @Test
    void drivingLicenses_removedClass_doesNotSatisfyPrerequisite() {
        TenantQualificationDefinition definition =
                definitionWithDrivingLicenses(QualificationRequirementMode.ALL, "DL_C");
        stubDrivingLicenseDefinitions();
        when(drivingLicenseRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.PREREQUISITES_MISSING);
    }

    @Test
    void drivingLicenses_validSubmission_satisfiesPrerequisite() {
        TenantQualificationDefinition definition =
                definitionWithDrivingLicenses(QualificationRequirementMode.ALL, "DL_C");
        stubDrivingLicenseDefinitions();
        stubGrantedDrivingLicenses(null, null, DL_CLASS_C);

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.CAN_EXERCISE);
    }

    @Test
    void certDefinitions_anyMode_acceptsOneValidCertification() {
        TenantQualificationDefinition definition =
                definitionWithCertifications(QualificationRequirementMode.ANY, "CERT_A", "CERT_B");
        stubCertDefinitions();
        when(certificationStatusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .items(
                                        List.of(
                                                certStatus(CERT_A, "EXPIRING_SOON"),
                                                certStatus(CERT_B, "VALID")))
                                .build());

        QualificationExerciseStatus status =
                statusService.evaluateStatus(definition, ORG_ID, USER_ID);

        assertThat(status).isEqualTo(QualificationExerciseStatus.CAN_EXERCISE);
    }

    private TenantQualificationDefinition definitionWithTrainingCourses(
            QualificationRequirementMode mode, String... configKeys) {
        TenantQualificationDefinition definition =
                TenantQualificationDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .configKey("QUAL")
                        .name("Qualification")
                        .requiresOrgDecision(false)
                        .trainingCourseRequirementMode(mode)
                        .build();
        for (String configKey : configKeys) {
            definition
                    .getTrainingCourses()
                    .add(
                            TenantQualificationTrainingCourse.builder()
                                    .tenantId(TENANT_ID)
                                    .qualificationDefinition(definition)
                                    .trainingCourseConfigKey(configKey)
                                    .build());
        }
        return definition;
    }

    private TenantQualificationDefinition definitionWithCertifications(
            QualificationRequirementMode mode, String... configKeys) {
        TenantQualificationDefinition definition =
                TenantQualificationDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .configKey("QUAL")
                        .name("Qualification")
                        .requiresOrgDecision(false)
                        .certRequirementMode(mode)
                        .build();
        for (String configKey : configKeys) {
            definition
                    .getCertDefinitions()
                    .add(
                            TenantQualificationCertDefinition.builder()
                                    .tenantId(TENANT_ID)
                                    .qualificationDefinition(definition)
                                    .certDefinitionConfigKey(configKey)
                                    .build());
        }
        return definition;
    }

    private void stubTrainingCourseTypes() {
        when(trainingCourseTypeRepository.findByConfigKeyIn(List.of("TC_A", "TC_B")))
                .thenReturn(
                        List.of(
                                TenantTrainingCourseType.builder()
                                        .id(TC_TYPE_A)
                                        .configKey("TC_A")
                                        .name("TC A")
                                        .build(),
                                TenantTrainingCourseType.builder()
                                        .id(TC_TYPE_B)
                                        .configKey("TC_B")
                                        .name("TC B")
                                        .build()));
        when(trainingCourseTypeRepository.findByConfigKey(anyString()))
                .thenAnswer(
                        invocation -> {
                            String key = invocation.getArgument(0);
                            return switch (key) {
                                case "TC_A" ->
                                        Optional.of(
                                                TenantTrainingCourseType.builder()
                                                        .id(TC_TYPE_A)
                                                        .configKey("TC_A")
                                                        .name("TC A")
                                                        .build());
                                case "TC_B" ->
                                        Optional.of(
                                                TenantTrainingCourseType.builder()
                                                        .id(TC_TYPE_B)
                                                        .configKey("TC_B")
                                                        .name("TC B")
                                                        .build());
                                default -> Optional.empty();
                            };
                        });
    }

    private void stubGrantedTrainingCourses(long... typeIds) {
        List<TrainingCourseUserSubmissionGrantedType> grantedTypes =
                java.util.Arrays.stream(typeIds)
                        .mapToObj(
                                typeId ->
                                        TrainingCourseUserSubmissionGrantedType.builder()
                                                .tenantTrainingCourseTypeId(typeId)
                                                .build())
                        .toList();
        when(submissionRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                TrainingCourseUserSubmission.builder()
                                        .userId(USER_ID)
                                        .grantedTypes(grantedTypes)
                                        .build()));
    }

    private TenantQualificationDefinition definitionWithDrivingLicenses(
            QualificationRequirementMode mode, String... configKeys) {
        TenantQualificationDefinition definition =
                TenantQualificationDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .configKey("QUAL")
                        .name("Qualification")
                        .requiresOrgDecision(false)
                        .drivingLicenseRequirementMode(mode)
                        .build();
        for (String configKey : configKeys) {
            definition
                    .getDrivingLicenses()
                    .add(
                            TenantQualificationDrivingLicense.builder()
                                    .tenantId(TENANT_ID)
                                    .qualificationDefinition(definition)
                                    .drivingLicenseConfigKey(configKey)
                                    .build());
        }
        return definition;
    }

    private void stubDrivingLicenseDefinitions() {
        when(drivingLicenseDefinitionRepository.findByConfigKeyIn(List.of("DL_C")))
                .thenReturn(
                        List.of(
                                TenantDrivingLicenseDefinition.builder()
                                        .id(DL_CLASS_C)
                                        .configKey("DL_C")
                                        .name("C")
                                        .build()));
        when(drivingLicenseDefinitionRepository.findByConfigKey("DL_C"))
                .thenReturn(
                        Optional.of(
                                TenantDrivingLicenseDefinition.builder()
                                        .id(DL_CLASS_C)
                                        .configKey("DL_C")
                                        .name("C")
                                        .build()));
    }

    private void stubGrantedDrivingLicenses(
            LocalDate documentExpirationDate,
            LocalDate classExpirationDate,
            long... definitionIds) {
        UserDrivingLicense license =
                UserDrivingLicense.builder()
                        .userId(USER_ID)
                        .documentExpirationDate(documentExpirationDate)
                        .issueDate(LocalDate.now())
                        .build();
        for (long definitionId : definitionIds) {
            license.getClasses()
                    .add(
                            UserDrivingLicenseClass.builder()
                                    .tenantDrivingLicenseDefinitionId(definitionId)
                                    .expirationDate(classExpirationDate)
                                    .license(license)
                                    .tenantId(TENANT_ID)
                                    .build());
        }
        when(drivingLicenseRepository.findByUserId(USER_ID)).thenReturn(Optional.of(license));
    }

    private void stubCertDefinitions() {
        when(certDefinitionRepository.findByConfigKeyIn(List.of("CERT_A", "CERT_B")))
                .thenReturn(
                        List.of(
                                TenantCertDefinition.builder()
                                        .id(CERT_A)
                                        .configKey("CERT_A")
                                        .name("Cert A")
                                        .build(),
                                TenantCertDefinition.builder()
                                        .id(CERT_B)
                                        .configKey("CERT_B")
                                        .name("Cert B")
                                        .build()));
        when(certDefinitionRepository.findByConfigKey(anyString()))
                .thenAnswer(
                        invocation -> {
                            String key = invocation.getArgument(0);
                            return switch (key) {
                                case "CERT_A" ->
                                        Optional.of(
                                                TenantCertDefinition.builder()
                                                        .id(CERT_A)
                                                        .configKey("CERT_A")
                                                        .name("Cert A")
                                                        .build());
                                case "CERT_B" ->
                                        Optional.of(
                                                TenantCertDefinition.builder()
                                                        .id(CERT_B)
                                                        .configKey("CERT_B")
                                                        .name("Cert B")
                                                        .build());
                                default -> Optional.empty();
                            };
                        });
    }
}
