/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.workflow.domain.ParticipantRole;
import com.emberone.workflow.domain.PrincipalType;
import com.emberone.workflow.spi.ParticipantSpec;
import org.junit.jupiter.api.Test;

class SlotKeysTest {

    @Test
    void forApproverOrgPrefixesOrganizationId() {
        assertThat(SlotKeys.forApproverOrg(42L)).isEqualTo("AGR:42");
    }

    @Test
    void forAdditionalUsesExplicitSlotKeyWhenProvided() {
        ParticipantSpec spec =
                ParticipantSpec.builder()
                        .role(ParticipantRole.VIEWER)
                        .principalType(PrincipalType.USER)
                        .principalId(1L)
                        .slotKey("custom-slot")
                        .build();

        assertThat(SlotKeys.forAdditional(spec)).isEqualTo("custom-slot");
    }

    @Test
    void forAdditionalBuildsDeterministicKeyFromSpecFields() {
        ParticipantSpec spec =
                ParticipantSpec.builder()
                        .role(ParticipantRole.APPROVER)
                        .principalType(PrincipalType.USER)
                        .principalId(3L)
                        .organizationId(9L)
                        .build();

        assertThat(SlotKeys.forAdditional(spec)).isEqualTo("ADD:APPROVER:USER:3:9");
    }
}
