/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;

public record OnboardingDocumentDefinitionBaseline(
        String configKey,
        String name,
        String description,
        OnboardingDocumentFulfillmentMode fulfillmentMode,
        boolean mandatory,
        boolean active) {}
