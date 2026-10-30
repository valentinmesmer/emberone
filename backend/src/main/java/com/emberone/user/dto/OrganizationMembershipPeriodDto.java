/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/** One organization membership interval of the current user. */
@Value
@Builder
public class OrganizationMembershipPeriodDto {
    Long organizationId;
    String organizationName;
    String organizationCode;
    boolean main;
    Instant startDate;
    Instant endDate;
    boolean active;
}
