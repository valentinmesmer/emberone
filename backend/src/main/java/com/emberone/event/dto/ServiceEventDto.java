/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.dto;

import com.emberone.event.domain.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ServiceEventDto extends BaseEventDto {
    private String topic;

    @Override
    public EventType getEventType() {
        return EventType.SERVICE;
    }
}
