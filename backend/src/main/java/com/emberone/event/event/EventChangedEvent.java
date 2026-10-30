/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.event;

import com.emberone.event.domain.EventType;

/** Published when a service or operation event is created or updated. */
public record EventChangedEvent(
        Long tenantId, Long organizationId, EventType eventType, Long eventId) {}
