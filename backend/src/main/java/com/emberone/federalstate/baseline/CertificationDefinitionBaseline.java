/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
