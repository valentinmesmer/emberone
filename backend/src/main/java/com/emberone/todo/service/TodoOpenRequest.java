/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.service;

import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.todo.domain.TodoSourceEntityType;
import com.emberone.todo.domain.TodoType;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

@Builder
public record TodoOpenRequest(
        Long tenantId,
        TodoType type,
        Long assigneeUserId,
        Set<Long> organizationGroupIds,
        /**
         * Users excluded from visibility (empty/null for most todos, including system-triggered
         * ones).
         */
        Set<Long> excludedUserIds,
        Map<String, Object> params,
        AppLink link,
        TodoSourceEntityType sourceEntityType,
        Long sourceEntityId,
        /**
         * When true, {@link com.emberone.todo.event.TodoCreatedEvent} skips {@code TODO_ASSIGNED}
         * (claim / restore of a personal approval todo). Not persisted on the todo.
         */
        boolean suppressCreateNotification) {}
