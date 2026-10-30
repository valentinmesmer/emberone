/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.workflow.domain.WorkflowMediaSource;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDocumentPromotionItemDto {

    private Long attachmentId;
    private WorkflowMediaSource source;
    private Long commentId;
    private EntityAttachmentDto attachment;

    /** Current selection (from the latest approver, or handler defaults). */
    private boolean selected;

    private String defaultTitle;
    private String defaultDescription;

    @Builder.Default private List<String> defaultTags = List.of();
}
