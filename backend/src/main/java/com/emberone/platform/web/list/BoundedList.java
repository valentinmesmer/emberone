/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a GET handler that intentionally returns an unbounded collection (catalog or naturally
 * limited sub-resource). All other list GETs must return {@link com.emberone.platform.web.PageDto}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BoundedList {

    /** Human-readable reason (enforced by convention test in a later work package). */
    String value();
}
