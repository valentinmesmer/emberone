/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** Manually create a certification record for a user ({@code appLinkType} stays null). */
@Data
public class CreateUserCertificationRecordRequest {

    @NotNull(message = "tenantCertDefinitionId is required") private Long tenantCertDefinitionId;

    @NotNull(message = "completedDate is required") private LocalDate completedDate;

    /** Optional explicit expiry; when null it is derived from the definition's interval. */
    private LocalDate expiryDate;

    private List<Long> userDocumentIds;

    private String notes;
}
