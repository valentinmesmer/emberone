/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import com.emberone.certification.domain.CertGrantRecipientMode;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CreateCertificationEventGrantRequest {

    @NotNull private Long tenantCertDefinitionId;

    @NotNull private CertGrantRecipientMode recipientMode;

    private String notes;

    /** Required when {@link CertGrantRecipientMode#SPECIFIC}. */
    private List<Long> recipientUserIds;
}
