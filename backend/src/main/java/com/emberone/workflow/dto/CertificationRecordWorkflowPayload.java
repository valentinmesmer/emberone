/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#CERTIFICATION_RECORD}: a single
 * certification definition, a completion date, optional notes, and proof drafts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRecordWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;

    @NotNull private Long tenantCertDefinitionId;

    @NotNull private LocalDate completedDate;

    @Size(max = 2048) private String notes;

    /** Proof documents; at least one is required for the workflow submission path. */
    @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
