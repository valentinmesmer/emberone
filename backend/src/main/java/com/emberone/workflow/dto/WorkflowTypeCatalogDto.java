/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.dto;

import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowOpenScope;
import com.emberone.workflow.domain.WorkflowRoutingDimension;
import java.util.List;
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

    /**
     * True when {@code allowsMultipleConcurrentOpen} is false and the current user already has an
     * active instance of this type.
     */
    private boolean typeBlocked;

    /**
     * The catalog the type can be split by in tenant-wide routing rules, or {@code null} when only
     * the default rule exists.
     */
    private WorkflowRoutingDimension routingDimension;

    /**
     * The approver slot roles of the type in display order, {@code CONTEXT} first and {@code
     * ADDITIONAL} last. The standard configures group and approval flags per role.
     */
    private List<String> approverSlotRoles;
}
