/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.retention.domain;

/** DIN 66398-style retention class: one category, one period, one handler. */
public enum RetentionCategory {
    EXIT_IMMEDIATE,
    EXIT_SETTLEMENT,
    DRIVING_LICENSE,
    EVIDENCE,
    ABSENCE,
    TIME_ENTRY,
    EVENT,
    APPOINTMENT,
    WORKFLOW_STALE,
    WORKFLOW,
    TODO,
    NOTIFICATION,
    ONBOARDING,
    AUDIT,
    USER_MASTER,
    MANAGEMENT_ACCESS
}
