/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Default title, description, and tags for workflow-media copied into the opener's document folder
 * when no approver saved explicit promotion selections (e.g. auto-approve on open).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDocumentPromotionMetadata {

    private String title;
    private String description;

    @Builder.Default private List<String> tags = List.of();
}
