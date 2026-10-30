/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.dto;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantOnboardingDocumentDefinitionDto {
    long id;
    String configKey;
    String abbreviation;
    String name;
    String description;
    OnboardingDocumentFulfillmentMode fulfillmentMode;
    boolean mandatory;
    boolean active;
    boolean archived;
    boolean deletable;
    boolean archivable;
    boolean hasTemplate;
    String templateFileName;
    Long templateSizeBytes;
}
