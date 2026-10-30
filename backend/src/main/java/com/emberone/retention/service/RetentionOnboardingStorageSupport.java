/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RetentionOnboardingStorageSupport {

    private final OnboardingInvitationStepDataRepository stepDataRepository;

    public List<String> findAttachmentStorageKeysForInvitations(List<Long> invitationIds) {
        if (invitationIds == null || invitationIds.isEmpty()) {
            return List.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Long invitationId : invitationIds) {
            for (OnboardingInvitationStepData data :
                    stepDataRepository.findByInvitationId(invitationId)) {
                collectKeysFromPayload(data.getPayload(), keys);
            }
        }
        return new ArrayList<>(keys);
    }

    private static void collectKeysFromPayload(Map<String, Object> payload, Set<String> keys) {
        if (payload == null) {
            return;
        }
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            collectAttachmentKeys(document, keys);
        }
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            collectAttachmentKeys(record, keys);
        }
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        if (license != null) {
            collectAttachmentKeys(license, keys);
        }
    }

    private static void collectAttachmentKeys(Map<String, Object> stored, Set<String> keys) {
        for (Map<String, Object> attachment : OnboardingStepPayloads.readAttachments(stored)) {
            String storageKey =
                    OnboardingStepPayloads.readNullableString(
                            attachment, OnboardingStepPayloads.STORAGE_KEY);
            if (StringUtils.hasText(storageKey)) {
                keys.add(storageKey);
            }
        }
    }
}
