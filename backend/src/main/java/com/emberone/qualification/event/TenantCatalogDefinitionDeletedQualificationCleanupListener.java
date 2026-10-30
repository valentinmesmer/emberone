/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.event;

import com.emberone.certification.event.TenantCertDefinitionDeletedEvent;
import com.emberone.drivinglicense.event.TenantDrivingLicenseDefinitionDeletedEvent;
import com.emberone.qualification.tenant.TenantQualificationCertDefinitionRepository;
import com.emberone.qualification.tenant.TenantQualificationDrivingLicenseRepository;
import com.emberone.qualification.tenant.TenantQualificationTrainingCourseRepository;
import com.emberone.trainingcourse.event.TenantTrainingCourseTypeDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Removes qualification prerequisite links when a tenant catalog definition is hard-deleted. Runs
 * in the same transaction as the catalog delete.
 */
@Service
@RequiredArgsConstructor
public class TenantCatalogDefinitionDeletedQualificationCleanupListener {

    private final TenantQualificationCertDefinitionRepository qualificationCertDefinitionRepository;
    private final TenantQualificationDrivingLicenseRepository qualificationDrivingLicenseRepository;
    private final TenantQualificationTrainingCourseRepository qualificationTrainingCourseRepository;

    @EventListener
    public void onCertDefinitionDeleted(TenantCertDefinitionDeletedEvent event) {
        qualificationCertDefinitionRepository.deleteByCertDefinitionConfigKey(event.configKey());
    }

    @EventListener
    public void onDrivingLicenseDefinitionDeleted(
            TenantDrivingLicenseDefinitionDeletedEvent event) {
        qualificationDrivingLicenseRepository.deleteByDrivingLicenseConfigKey(event.configKey());
    }

    @EventListener
    public void onTrainingCourseTypeDeleted(TenantTrainingCourseTypeDeletedEvent event) {
        qualificationTrainingCourseRepository.deleteByTrainingCourseConfigKey(event.configKey());
    }
}
