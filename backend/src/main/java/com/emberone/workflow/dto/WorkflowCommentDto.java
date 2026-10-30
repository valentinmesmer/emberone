/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.workflow.domain.WorkflowCommentAuthorRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCommentDto {
    private Long id;
    private Long userId;
    private String userFirstName;
    private String userLastName;

    /** Resolved display name for list and detail responses. */
    private String userDisplayName;

    private WorkflowCommentAuthorRole authorRole;
    private String body;
    private EntityAttachmentDto attachment;
}
