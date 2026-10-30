/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.drivinglicense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@link com.emberone.workflow.domain.WorkflowType#DRIVING_LICENSE_RECORD}: driving
 * license document fields, one or more classes, optional notes, and proof document drafts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrivingLicenseRecordWorkflowPayload {

    public static final int MAX_ATTACHMENTS = 10;

    @NotNull private LocalDate issueDate;

    private LocalDate documentExpirationDate;

    @NotBlank @Size(max = 128) private String licenseNumber;

    @NotBlank @Size(max = 512) private String issuingAuthority;

    @NotEmpty private List<@Valid DrivingLicenseClassPayload> classes;

    @Size(max = 2048) private String notes;

    /** Proof documents; at least one is required for the workflow submission path. */
    @NotEmpty @Size(max = MAX_ATTACHMENTS) private List<@NotNull Long> attachmentIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrivingLicenseClassPayload {

        @NotNull private Long tenantDrivingLicenseDefinitionId;

        private LocalDate grantDate;

        private LocalDate expirationDate;
    }
}
