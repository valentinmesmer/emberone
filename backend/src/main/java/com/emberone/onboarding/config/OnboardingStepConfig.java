/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.config;

/** Typed configuration payload for an onboarding step. */
public sealed interface OnboardingStepConfig
        permits EntryDocumentsStepConfig, EmergencyContactsStepConfig, EmptyStepConfig {}
