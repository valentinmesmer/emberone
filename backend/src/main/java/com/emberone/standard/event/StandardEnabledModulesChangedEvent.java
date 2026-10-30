/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.event;

/** Published when a standard's enabled-module set changes or the standard is deleted. */
public record StandardEnabledModulesChangedEvent(Long tenantId, Long standardId) {}
