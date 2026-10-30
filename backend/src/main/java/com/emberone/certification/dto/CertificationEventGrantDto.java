/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import com.emberone.certification.domain.CertGrantRecipientMode;
import com.emberone.event.domain.EventType;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CertificationEventGrantDto {
    Long id;
    Long organizationId;
    EventType eventType;
    Long eventId;
    Long tenantCertDefinitionId;
    String certDefinitionName;
    CertGrantRecipientMode recipientMode;
    LocalDate completedDate;
    String notes;
    Long createdByUserId;
    int recipientCount;
    int recordCount;
}
