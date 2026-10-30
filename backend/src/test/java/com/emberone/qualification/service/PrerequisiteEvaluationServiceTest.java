/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.repository.UserDrivingLicenseRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.qualification.service.PrerequisiteEvaluationService.Holdings;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrerequisiteEvaluationServiceTest {

    private static final long TENANT_ID = 7L;

    @Mock CertificationStatusService certificationStatusService;
    @Mock TrainingCourseUserSubmissionRepository submissionRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    @Mock UserDrivingLicenseRepository drivingLicenseRepository;
    @Mock TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    @Mock TenantCertDefinitionRepository certDefinitionRepository;

    @InjectMocks PrerequisiteEvaluationService service;

    @Test
    void noPrerequisitesAreAlwaysMet() {
        Holdings holdings = new Holdings(Set.of(), Set.of(), Map.of());

        assertThat(
                        service.trainingCoursesMet(
                                List.of(), QualificationRequirementMode.ALL, holdings))
                .isTrue();
        assertThat(
                        service.certDefinitionsMet(
                                List.of(), QualificationRequirementMode.ANY, holdings))
                .isTrue();
        assertThat(
                        service.drivingLicensesMet(
                                List.of(), QualificationRequirementMode.ALL, holdings))
                .isTrue();
    }

    @Test
    void trainingCoursesAllVersusAny() {
        when(trainingCourseTypeRepository.findByConfigKeyIn(List.of("A", "B")))
                .thenReturn(List.of(type(1L, "A"), type(2L, "B")));
        Holdings holdsOnlyA = new Holdings(Set.of(1L), Set.of(), Map.of());

        assertThat(
                        service.trainingCoursesMet(
                                List.of("A", "B"), QualificationRequirementMode.ALL, holdsOnlyA))
                .isFalse();
        assertThat(
                        service.trainingCoursesMet(
                                List.of("A", "B"), QualificationRequirementMode.ANY, holdsOnlyA))
                .isTrue();
    }

    @Test
    void unknownKeysAreNeverMet() {
        when(trainingCourseTypeRepository.findByConfigKeyIn(List.of("X"))).thenReturn(List.of());

        assertThat(
                        service.trainingCoursesMet(
                                List.of("X"),
                                QualificationRequirementMode.ANY,
                                new Holdings(Set.of(1L), Set.of(), Map.of())))
                .isFalse();
    }

    @Test
    void onlyValidCertificationsCount() {
        TenantCertDefinition cert = new TenantCertDefinition();
        cert.setId(5L);
        when(certDefinitionRepository.findByConfigKeyIn(List.of("G26"))).thenReturn(List.of(cert));

        assertThat(
                        service.certDefinitionsMet(
                                List.of("G26"),
                                QualificationRequirementMode.ALL,
                                new Holdings(Set.of(), Set.of(), Map.of(5L, certStatus("VALID")))))
                .isTrue();
        assertThat(
                        service.certDefinitionsMet(
                                List.of("G26"),
                                QualificationRequirementMode.ALL,
                                new Holdings(
                                        Set.of(), Set.of(), Map.of(5L, certStatus("EXPIRED")))))
                .isFalse();
    }

    private static TenantTrainingCourseType type(long id, String configKey) {
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(id);
        type.setConfigKey(configKey);
        return type;
    }

    private static CertificationStatusItemDto certStatus(String status) {
        return CertificationStatusItemDto.builder().status(status).build();
    }
}
