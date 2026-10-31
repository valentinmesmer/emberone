/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.dto;

import com.emberone.event.domain.EventType;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEventDto {

    private Long id;

    @OrganizationWallClock private Instant start;

    @OrganizationWallClock private Instant end;

    private Long organizationId;
    private String location;
    private String description;
    private Long leaderId;
    private EventType eventType;
}
