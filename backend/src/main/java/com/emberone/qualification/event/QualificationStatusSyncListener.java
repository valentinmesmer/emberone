/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.event;

import com.emberone.certification.event.CertificationAssignmentChangedEvent;
import com.emberone.drivinglicense.event.DrivingLicenseRecordChangedEvent;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.qualification.service.QualificationStatusSyncService;
import com.emberone.trainingcourse.event.TrainingCourseRecordChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QualificationStatusSyncListener {

    private final QualificationStatusSyncService syncService;

    @AfterCommitEventListener
    public void onCertificationAssignmentChanged(CertificationAssignmentChangedEvent event) {
        syncService.syncForUser(event.tenantId(), event.userId());
    }

    @AfterCommitEventListener
    public void onTrainingCourseRecordChanged(TrainingCourseRecordChangedEvent event) {
        syncService.syncForUser(event.tenantId(), event.userId());
    }

    @AfterCommitEventListener
    public void onDrivingLicenseRecordChanged(DrivingLicenseRecordChangedEvent event) {
        syncService.syncForUser(event.tenantId(), event.userId());
    }

    @AfterCommitEventListener
    public void onQualificationOrgDecisionChanged(QualificationOrgDecisionChangedEvent event) {
        syncService.syncForUser(event.tenantId(), event.userId());
    }

    @AfterCommitEventListener
    public void onQualificationDefinitionChanged(QualificationDefinitionChangedEvent event) {
        syncService.syncForQualification(event.tenantId(), event.qualificationDefinitionId());
    }

    @AfterCommitEventListener
    public void onQualificationCatalogChanged(QualificationCatalogChangedEvent event) {
        syncService.syncForCatalogChange(event.tenantId(), event.organizationIds());
    }
}
