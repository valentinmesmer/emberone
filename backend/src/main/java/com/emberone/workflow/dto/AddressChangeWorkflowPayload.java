/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#ADDRESS_CHANGE}: requested address
 * and optional attachment drafts uploaded beforehand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressChangeWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;

    @NotBlank @Size(max = 255) private String street;

    @NotBlank @Size(max = 255) private String city;

    @NotBlank @Size(max = 32) private String postalCode;

    @NotBlank @Size(max = 255) private String country;

    /**
     * Snapshot of the requester's address at open time; set by the server, not sent by the client.
     */
    @Size(max = 255) private String previousStreet;

    @Size(max = 255) private String previousCity;

    @Size(max = 32) private String previousPostalCode;

    @Size(max = 255) private String previousCountry;

    /** Optional proof documents; may be empty. */
    @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;
}
