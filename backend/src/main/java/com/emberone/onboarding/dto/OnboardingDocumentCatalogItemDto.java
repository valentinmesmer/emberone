/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboarding.dto;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingDocumentCatalogItemDto {
    String configKey;
    String name;
    String description;
    OnboardingDocumentFulfillmentMode fulfillmentMode;
    boolean mandatory;
    boolean hasTemplate;
    String templateFileName;
    Integer templateVersion;
    Instant templateUploadedAt;
    boolean alreadyFulfilled;

    /** Live user document after promote (signed uploads on approved invitations). */
    Long promotedUserDocumentId;
}
