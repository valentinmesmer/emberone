/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
