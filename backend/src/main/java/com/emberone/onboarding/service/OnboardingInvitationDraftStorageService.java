/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Deletes onboarding invitation draft files from the attachments bucket (process copies). */
@Service
@RequiredArgsConstructor
public class OnboardingInvitationDraftStorageService {

    private final OnboardingInvitationStepDataRepository stepDataRepository;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    public void deleteDraftFilesForInvitation(Long invitationId) {
        for (OnboardingInvitationStepData data :
                stepDataRepository.findByInvitationId(invitationId)) {
            deleteAttachmentsInPayload(data.getPayload());
        }
    }

    private void deleteAttachmentsInPayload(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            deleteAttachments(document);
        }
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            deleteAttachments(record);
        }
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        if (license != null) {
            deleteAttachments(license);
        }
    }

    private void deleteAttachments(Map<String, Object> stored) {
        for (Map<String, Object> attachment : OnboardingStepPayloads.readAttachments(stored)) {
            deleteStoredUpload(attachment);
        }
    }

    private void deleteStoredUpload(Map<String, Object> attachment) {
        String storageKey =
                OnboardingStepPayloads.readNullableString(
                        attachment, OnboardingStepPayloads.STORAGE_KEY);
        if (storageKey == null) {
            return;
        }
        objectStorageTransactionCoordinator.deleteFileAfterCommit(attachmentsBucket, storageKey);
    }
}
