/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowOpenScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTypeCatalogDto {
    /** Stable numeric id (matches {@link com.emberone.workflow.domain.WorkflowType#getId()}). */
    private Long workflowTypeId;

    /** Enum constant name, e.g. {@code NAME_CHANGE}. */
    private String workflowTypeKey;

    private WorkflowMode mode;

    /** When false, the user may not open another instance while one is {@code OPEN}. */
    private boolean allowsMultipleConcurrentOpen;

    /** {@link WorkflowOpenScope#SELF} (Mein Bereich) or {@link WorkflowOpenScope#ORGANIZATION}. */
    private WorkflowOpenScope openScope;

    /**
     * When {@code true}, the type appears under Self Anträge → Hinzufügen. When {@code false}, the
     * type is beginnable only from a dedicated entry point (still returned so clients can gate
     * those UIs).
     */
    private boolean listedInBeginnableCatalog;
}
