/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateOperationEventRequest extends BaseCreateEventRequest {

    @NotBlank(message = "Number is required") private String number;

    @NotBlank(message = "Keyword is required") private String keyword;

    @NotBlank(message = "Tag is required") private String tag;

    @NotNull(message = "Special signal is required") private Boolean specialSignal;
}
