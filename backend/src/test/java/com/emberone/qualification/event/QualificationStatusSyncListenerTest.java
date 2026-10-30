/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.event;

import static org.mockito.Mockito.verify;

import com.emberone.certification.event.CertificationAssignmentChangedEvent;
import com.emberone.qualification.service.QualificationStatusSyncService;
import com.emberone.trainingcourse.event.TrainingCourseRecordChangedEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QualificationStatusSyncListenerTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final long DEFINITION_ID = 100L;

    @Mock QualificationStatusSyncService syncService;

    @InjectMocks QualificationStatusSyncListener listener;

    @Test
    void onCertificationAssignmentChanged_syncsUser() {
        listener.onCertificationAssignmentChanged(
                new CertificationAssignmentChangedEvent(TENANT_ID, USER_ID));

        verify(syncService).syncForUser(TENANT_ID, USER_ID);
    }

    @Test
    void onTrainingCourseRecordChanged_syncsUser() {
        listener.onTrainingCourseRecordChanged(
                new TrainingCourseRecordChangedEvent(TENANT_ID, USER_ID));

        verify(syncService).syncForUser(TENANT_ID, USER_ID);
    }

    @Test
    void onQualificationOrgDecisionChanged_syncsUser() {
        listener.onQualificationOrgDecisionChanged(
                new QualificationOrgDecisionChangedEvent(TENANT_ID, USER_ID, DEFINITION_ID));

        verify(syncService).syncForUser(TENANT_ID, USER_ID);
    }

    @Test
    void onQualificationDefinitionChanged_syncsQualification() {
        listener.onQualificationDefinitionChanged(
                new QualificationDefinitionChangedEvent(TENANT_ID, DEFINITION_ID));

        verify(syncService).syncForQualification(TENANT_ID, DEFINITION_ID);
    }

    @Test
    void onQualificationCatalogChanged_syncsCatalog() {
        listener.onQualificationCatalogChanged(
                new QualificationCatalogChangedEvent(TENANT_ID, List.of(10L, 11L)));

        verify(syncService).syncForCatalogChange(TENANT_ID, List.of(10L, 11L));
    }
}
