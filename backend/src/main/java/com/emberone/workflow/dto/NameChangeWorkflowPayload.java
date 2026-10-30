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
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#NAME_CHANGE}: requested name and
 * attachment drafts uploaded beforehand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameChangeWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;

    @NotBlank @Size(max = 255) private String firstName;

    @NotBlank @Size(max = 255) private String lastName;

    /** Snapshot of the requester's name at open time; set by the server, not sent by the client. */
    @Size(max = 255) private String previousFirstName;

    @Size(max = 255) private String previousLastName;

    @NotEmpty @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
