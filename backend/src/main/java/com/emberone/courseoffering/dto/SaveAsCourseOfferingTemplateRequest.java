/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveAsCourseOfferingTemplateRequest {

    @NotBlank(message = "Template name is required") @Size(max = 255, message = "Template name must be at most 255 characters") private String templateName;
}
