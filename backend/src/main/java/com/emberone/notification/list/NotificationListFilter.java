/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.list;

import com.emberone.notification.domain.NotificationType;
import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

/**
 * Self notification list filters. Repeated {@code status} and {@code type} params are OR-combined
 * within each dimension; empty {@code status} means no read/unread restriction.
 */
public record NotificationListFilter(
        List<NotificationListStatus> statuses, List<NotificationType> types) {

    public NotificationListFilter {
        if (statuses == null) {
            statuses = List.of();
        }
        if (types == null) {
            types = List.of();
        }
    }

    public static NotificationListFilter parse(List<String> status, List<String> type) {
        return new NotificationListFilter(
                ListFilters.parseEnumList(
                        status,
                        "status",
                        raw -> {
                            try {
                                return Optional.of(NotificationListStatus.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }),
                ListFilters.parseEnumList(
                        type,
                        "type",
                        raw -> {
                            try {
                                return Optional.of(NotificationType.valueOf(raw));
                            } catch (IllegalArgumentException ex) {
                                return Optional.empty();
                            }
                        }));
    }
}
