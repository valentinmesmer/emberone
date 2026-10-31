/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.domain;

/**
 * Origin of a configurable item. The system seeds read-only {@link #SYSTEM} baselines; {@link
 * #CUSTOM} denotes tenant-created content (e.g. cloned org standards, custom cert definitions).
 */
public enum StandardSource {
    SYSTEM,
    CUSTOM
}
