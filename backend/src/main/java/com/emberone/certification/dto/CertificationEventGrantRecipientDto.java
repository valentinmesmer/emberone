/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CertificationEventGrantRecipientDto {
    Long userId;
    String firstName;
    String lastName;
    boolean inScope;
    Long recordId;
    boolean edited;
}
