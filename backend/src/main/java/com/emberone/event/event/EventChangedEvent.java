/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.event;

import com.emberone.event.domain.EventType;

/** Published when a service or operation event is created or updated. */
public record EventChangedEvent(
        Long tenantId, Long organizationId, EventType eventType, Long eventId) {}
