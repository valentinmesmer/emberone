/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.event;

/** Published when a standard's enabled-module set changes or the standard is deleted. */
public record StandardEnabledModulesChangedEvent(Long tenantId, Long standardId) {}
