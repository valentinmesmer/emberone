/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.workflow.domain.WorkflowMediaSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDocumentPromotionSelection {

    @NotNull(message = "attachmentId is required") private Long attachmentId;

    @NotNull(message = "source is required") private WorkflowMediaSource source;

    @Size(max = 512, message = "Title must be at most 512 characters") private String title;

    @Size(max = 2048, message = "Description must be at most 2048 characters") private String description;

    @Size(max = 32, message = "At most 32 tags are allowed") private List<@Size(max = 128, message = "Tag must be at most 128 characters") String> tags;
}
