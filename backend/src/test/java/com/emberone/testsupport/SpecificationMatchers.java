/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.testsupport;

import static org.mockito.ArgumentMatchers.any;

import org.springframework.data.jpa.domain.Specification;

/**
 * Typed Mockito matchers for Spring Data specifications, so tests stay free of unchecked warnings.
 */
public final class SpecificationMatchers {

    private SpecificationMatchers() {}

    /** Matches any {@link Specification}, inferring the entity type from the call site. */
    @SuppressWarnings("unchecked")
    public static <T> Specification<T> anySpecification() {
        return any(Specification.class);
    }
}
