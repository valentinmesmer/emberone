/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.rankrequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for {@link com.emberone.workflow.domain.WorkflowType#RANK_REQUEST}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankRequestWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;

    @NotNull private Long tenantRankId;

    @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
