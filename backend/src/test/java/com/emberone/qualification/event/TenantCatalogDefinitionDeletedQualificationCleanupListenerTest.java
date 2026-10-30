/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.event;

import static org.mockito.Mockito.verify;

import com.emberone.certification.event.TenantCertDefinitionDeletedEvent;
import com.emberone.drivinglicense.event.TenantDrivingLicenseDefinitionDeletedEvent;
import com.emberone.qualification.tenant.TenantQualificationCertDefinitionRepository;
import com.emberone.qualification.tenant.TenantQualificationDrivingLicenseRepository;
import com.emberone.qualification.tenant.TenantQualificationTrainingCourseRepository;
import com.emberone.trainingcourse.event.TenantTrainingCourseTypeDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantCatalogDefinitionDeletedQualificationCleanupListenerTest {

    @Mock TenantQualificationCertDefinitionRepository qualificationCertDefinitionRepository;
    @Mock TenantQualificationDrivingLicenseRepository qualificationDrivingLicenseRepository;
    @Mock TenantQualificationTrainingCourseRepository qualificationTrainingCourseRepository;
    @InjectMocks TenantCatalogDefinitionDeletedQualificationCleanupListener listener;

    @Test
    void onCertDefinitionDeletedPurgesQualificationLinks() {
        listener.onCertDefinitionDeleted(new TenantCertDefinitionDeletedEvent(7L, "G26"));

        verify(qualificationCertDefinitionRepository).deleteByCertDefinitionConfigKey("G26");
    }

    @Test
    void onDrivingLicenseDefinitionDeletedPurgesQualificationLinks() {
        listener.onDrivingLicenseDefinitionDeleted(
                new TenantDrivingLicenseDefinitionDeletedEvent(7L, "B"));

        verify(qualificationDrivingLicenseRepository).deleteByDrivingLicenseConfigKey("B");
    }

    @Test
    void onTrainingCourseTypeDeletedPurgesQualificationLinks() {
        listener.onTrainingCourseTypeDeleted(new TenantTrainingCourseTypeDeletedEvent(7L, "TM"));

        verify(qualificationTrainingCourseRepository).deleteByTrainingCourseConfigKey("TM");
    }
}
