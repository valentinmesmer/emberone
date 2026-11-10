/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.event;

/** Published after a custom standard has been deleted. */
public record StandardDeletedEvent(Long tenantId, Long standardId) {}
