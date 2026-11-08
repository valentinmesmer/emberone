/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

/**
 * API surface a {@link HelpTopic} belongs to (mirrors {@code /my-area}, {@code /organizations},
 * {@code /admin}).
 */
public enum HelpTopicArea {
    SELF,
    ORGANIZATION,
    ADMIN
}
