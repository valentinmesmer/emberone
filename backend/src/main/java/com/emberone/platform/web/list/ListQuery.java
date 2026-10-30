/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import org.springframework.web.bind.annotation.RequestParam;

/** Shared pagination, search, and sort query parameters for list GET endpoints. */
public record ListQuery(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String sort) {}
