/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.domain;

import java.time.Instant;
import lombok.Builder;

@Builder
public record EntryDocumentFulfillmentProof(
        int templateVersion,
        String templateSha256Hex,
        String confirmationText,
        EntryDocumentFulfillmentOutcome outcome,
        EntryDocumentFulfillmentChannel channel,
        long actorUserId,
        String adminRecordReason,
        Long workflowReviewerUserId,
        Instant workflowReviewedAt,
        Instant confirmedAt) {

    public EntryDocumentFulfillmentProof {
        if (confirmationText == null) {
            confirmationText = "";
        }
    }

    public static EntryDocumentFulfillmentProof selfDecline(
            int templateVersion, String templateSha256Hex, long actorUserId) {
        return selfOutcome(
                templateVersion,
                templateSha256Hex,
                EntryDocumentFulfillmentOutcome.DECLINED,
                actorUserId);
    }

    private static EntryDocumentFulfillmentProof selfOutcome(
            int templateVersion,
            String templateSha256Hex,
            EntryDocumentFulfillmentOutcome outcome,
            long actorUserId) {
        return EntryDocumentFulfillmentProof.builder()
                .templateVersion(templateVersion)
                .templateSha256Hex(templateSha256Hex)
                .confirmationText("")
                .outcome(outcome)
                .channel(EntryDocumentFulfillmentChannel.SELF)
                .actorUserId(actorUserId)
                .confirmedAt(Instant.now())
                .build();
    }
}
