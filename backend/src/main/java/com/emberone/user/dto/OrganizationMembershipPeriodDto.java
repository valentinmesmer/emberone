/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
