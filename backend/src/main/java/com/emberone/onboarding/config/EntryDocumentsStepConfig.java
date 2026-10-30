/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
