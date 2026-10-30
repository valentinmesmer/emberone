/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateServiceEventRequest extends BaseCreateEventRequest {

    /**
     * When set, this appointment is archived after the event is created (same org, matching type
     * and times).
     */
    private Long promoteFromAppointmentId;

    @NotBlank(message = "Topic is required") private String topic;
}
