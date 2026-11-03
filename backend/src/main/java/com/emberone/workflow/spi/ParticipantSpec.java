/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.spi;

import com.emberone.workflow.domain.ParticipantRole;
import com.emberone.workflow.domain.PrincipalType;
import lombok.Builder;

/**
 * An additional participant (USER approver or viewer) contributed by a handler.
 *
 * @param slotKey optional stable slot key; required when the same role/principal combination can
 *     occur more than once.
 */
@Builder
public record ParticipantSpec(
        ParticipantRole role,
        PrincipalType principalType,
        Long principalId,
        Long organizationId,
        String slotKey) {}
