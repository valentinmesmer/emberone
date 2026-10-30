/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.dto.SelfCourseOfferingDto;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

/**
 * The detail of an offering is visible even if the user already holds its course type. The held
 * filter applies to the list only; a detail that hid it would give a 404 for a row the list shows.
 */
@ExtendWith(MockitoExtension.class)
class SelfCourseOfferingServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 3L;
    private static final long OFFERING_ID = 100L;
    private static final long TYPE_ID = 20L;

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock CourseAudienceScopeService scopeService;
    @Mock CoursePrerequisiteService prerequisiteService;
    @Mock TrainingCourseUserSubmissionRepository submissionRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock TokenService tokenService;
    @InjectMocks SelfCourseOfferingService service;

    @Test
    void detailOfHeldOfferingIsVisibleAndFlaggedAlreadyHeld() {
        CourseOffering offering =
                CourseOffering.builder()
                        .id(OFFERING_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(10L)
                        .trainingCourseTypeId(TYPE_ID)
                        .status(CourseOfferingStatus.PUBLISHED)
                        .build();
        when(submissionRepository.findGrantedTypeIdsByUser(USER_ID)).thenReturn(List.of(TYPE_ID));
        when(offeringRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(offering));
        when(trainingCourseTypeRepository.findById(TYPE_ID))
                .thenReturn(
                        Optional.of(
                                TenantTrainingCourseType.builder()
                                        .id(TYPE_ID)
                                        .name("Atemschutz")
                                        .build()));
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).name("Feuerwehr").build()));

        SelfCourseOfferingDto dto = service.get(USER_ID, OFFERING_ID);

        assertThat(dto.getId()).isEqualTo(OFFERING_ID);
        assertThat(dto.isAlreadyHeld()).isTrue();
    }
}
