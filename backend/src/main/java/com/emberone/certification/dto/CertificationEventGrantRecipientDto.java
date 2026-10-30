/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
