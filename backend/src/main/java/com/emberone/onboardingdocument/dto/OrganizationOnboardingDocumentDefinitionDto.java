/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboardingdocument.dto;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrganizationOnboardingDocumentDefinitionDto {
    long id;
    long organizationId;
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
