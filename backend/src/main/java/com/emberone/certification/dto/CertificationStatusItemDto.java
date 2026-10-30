/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/** One required certification for a user with its computed status. */
@Data
@Builder
public class CertificationStatusItemDto {
    private Long tenantCertDefinitionId;
    private String certDefinitionName;
    private String status;
    private boolean mandatory;
    private Long responsibleOrganizationId;
    private LocalDate completedDate;
    private LocalDate expiryDate;
    private Long recordId;
    private boolean archived;

    /** {@code false} when the user has historical records but no active assignment. */
    private boolean assigned;

    private Set<String> evidenceTypes;
}
