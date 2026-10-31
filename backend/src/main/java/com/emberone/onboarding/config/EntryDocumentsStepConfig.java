/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboarding.config;

import java.util.List;

public record EntryDocumentsStepConfig(List<String> documentConfigKeys)
        implements OnboardingStepConfig {

    public EntryDocumentsStepConfig {
        documentConfigKeys =
                documentConfigKeys == null ? List.of() : List.copyOf(documentConfigKeys);
    }
}
