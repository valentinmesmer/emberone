/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.event;

import java.util.Set;

/**
 * Published when specific users newly gain visibility of an already-open todo (group membership
 * join or assignee-group remap) so notification and realtime adapters can target only those users.
 */
public record TodoVisibilityGainedEvent(Long todoId, Set<Long> recipientUserIds) {}
