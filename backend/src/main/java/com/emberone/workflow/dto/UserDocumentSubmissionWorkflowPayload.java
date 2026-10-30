/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#USER_DOCUMENT_SUBMISSION}: document
 * metadata and attachment drafts uploaded beforehand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentSubmissionWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;

    @NotBlank @Size(max = 512) private String title;

    @Size(max = 2048) private String description;

    private List<@Size(min = 1, max = 128) String> tags;

    @NotEmpty @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
