/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

/**
 * @param intervalDays validity in days; {@code null} = never expires
 */
public record CertificationDefinitionBaseline(
        String configKey,
        String name,
        String description,
        Integer intervalDays,
        Integer reminderDaysBeforeExpiry,
        boolean active) {}
