/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.event;

import java.util.Set;

/**
 * Published when an open todo's assignee organization groups change so newly eligible members can
 * receive create-style notifications without re-blasting the whole group.
 */
public record TodoAssigneeGroupsChangedEvent(
        Long todoId, Set<Long> previousOrganizationGroupIds, Set<Long> newOrganizationGroupIds) {}
