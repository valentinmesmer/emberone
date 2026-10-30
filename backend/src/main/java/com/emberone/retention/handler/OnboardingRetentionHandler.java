/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.OnboardingRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
import com.emberone.retention.service.RetentionOnboardingStorageSupport;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import com.emberone.userdocument.service.UserDocumentService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of(
                    "onb_invitations",
                    "onb_invitation_step_data",
                    "onb_document_definitions",
                    "onb_document_definition_template_versions",
                    "onb_org_document_definitions",
                    "onb_org_document_definition_template_versions");

    private final OnboardingRetentionRepository onboardingRetentionRepository;
    private final RetentionOnboardingStorageSupport retentionOnboardingStorageSupport;
    private final RetentionObjectStorageSupport retentionObjectStorageSupport;
    private final UserDocumentService userDocumentService;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.ONBOARDING;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for retention");
        }
        List<Long> invitationIds =
                onboardingRetentionRepository.findApprovedInvitationIdsCompletedBefore(cutoff);
        List<String> storageKeys =
                retentionOnboardingStorageSupport.findAttachmentStorageKeysForInvitations(
                        invitationIds);
        for (String storageKey : storageKeys) {
            retentionObjectStorageSupport.enqueueAttachmentObject(tenantId, storageKey);
        }
        int deleted =
                userDocumentService.deleteDocumentsForLinkTargets(
                        tenantId, AppLinkType.ONBOARDING_INVITATION, invitationIds);
        return deleted
                + RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                onboardingRetentionRepository
                                        .deleteApprovedInvitationsCompletedBefore(cutoff));
    }
}
