/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCertificationRecordDto {
    private Long id;
    private Long userId;
    private Long tenantCertDefinitionId;
    private String certDefinitionName;
    private boolean definitionArchived;
    private LocalDate completedDate;
    private LocalDate expiryDate;
    private List<Long> userDocumentIds;
    private String notes;
    private String appLinkType;
    private Long appLinkId;
    private String submissionMethod;

    /** {@code SERVICE} / {@code OPERATION} when {@code submissionMethod=EVENT}. */
    private String eventType;

    /** Organization of the linked event when {@code submissionMethod=EVENT}. */
    private Long eventOrganizationId;

    private String navigationPath;
}
