/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import java.util.List;
import java.util.Map;

/** Filter options keyed by query param name (`tenantId`, `organizationId`, `appRole`, …). */
public record ListFilterOptionsDto(Map<String, List<ListFilterOptionDto>> options) {}
