/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.domain;

/**
 * How a certification record was submitted: {@code MANUAL}, {@code WORKFLOW}, {@code EVENT}, {@code
 * ONBOARDING}.
 */
public enum CertificationSubmissionMethod {
    MANUAL,
    WORKFLOW,
    EVENT,
    ONBOARDING
}
