/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.spi.ParticipantSpec;

/**
 * Deterministic slot keys (no DB surrogates) so dedupe at opening and later references stay stable.
 */
final class SlotKeys {

    private SlotKeys() {}

    static String forApproverOrg(Long organizationId) {
        return "AGR:" + organizationId;
    }

    static String forAdditional(ParticipantSpec spec) {
        if (spec.slotKey() != null && !spec.slotKey().isBlank()) {
            return spec.slotKey();
        }
        StringBuilder sb =
                new StringBuilder("ADD:")
                        .append(spec.role())
                        .append(':')
                        .append(spec.principalType())
                        .append(':')
                        .append(spec.principalId());
        if (spec.organizationId() != null) {
            sb.append(':').append(spec.organizationId());
        }
        return sb.toString();
    }
}
