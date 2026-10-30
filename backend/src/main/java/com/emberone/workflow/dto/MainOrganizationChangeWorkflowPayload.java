/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#MAIN_ORGANIZATION_CHANGE}: new main
 * organization and optional reason / attachments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainOrganizationChangeWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;
    public static final int MAX_REASON_LENGTH = 2000;

    @NotNull private Long newMainOrganizationId;

    /** Snapshot of the new main organization name at open time; set by the server. */
    @Size(max = 255) private String newMainOrganizationName;

    @Size(max = MAX_REASON_LENGTH) private String reason;

    /** Optional supporting documents; may be empty. */
    @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
