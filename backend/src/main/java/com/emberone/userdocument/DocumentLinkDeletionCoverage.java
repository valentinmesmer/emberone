/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument;

import com.emberone.platform.navigation.domain.AppLinkType;
import java.util.Map;
import java.util.Set;

/**
 * {@link AppLinkType} values that may appear in {@code doc_document_links.link_type}, and every
 * component that deletes such a target record (manually or by retention). Each of them must delete
 * the linked user documents in the same step.
 */
public final class DocumentLinkDeletionCoverage {

    private DocumentLinkDeletionCoverage() {}

    public static final Set<AppLinkType> DOCUMENT_LINK_TYPES =
            Set.of(
                    AppLinkType.WORKFLOW,
                    AppLinkType.CERTIFICATION_RECORD,
                    AppLinkType.TRAINING_COURSE_RECORD,
                    AppLinkType.DRIVING_LICENSE_RECORD,
                    AppLinkType.ONBOARDING_INVITATION);

    /**
     * Link types that never appear in {@code doc_document_links} (e.g. certification record
     * App-Links to event grants).
     */
    public static final Set<AppLinkType> NON_DOCUMENT_LINK_TYPES =
            Set.of(AppLinkType.CERTIFICATION_EVENT_GRANT, AppLinkType.COURSE_REGISTRATION);

    public static final Map<AppLinkType, Set<String>> DELETING_COMPONENTS =
            Map.of(
                    AppLinkType.DRIVING_LICENSE_RECORD,
                    Set.of("DrivingLicenseRecordService", "DrivingLicenseRetentionHandler"),
                    AppLinkType.TRAINING_COURSE_RECORD,
                    Set.of("TrainingCourseRecordService"),
                    AppLinkType.CERTIFICATION_RECORD,
                    Set.of("CertificationRecordService", "CertificationRecordRetentionSupport"),
                    AppLinkType.ONBOARDING_INVITATION,
                    Set.of("OnboardingInvitationService", "OnboardingRetentionHandler"),
                    AppLinkType.WORKFLOW,
                    Set.of("WorkflowRetentionHandler"));

    public static final String DELETE_DOCUMENTS_CALL = "userDocumentService.deleteDocuments";
}
