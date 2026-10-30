/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.list;

import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationSelfService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Filter options for {@code GET /api/v1/self/notifications}. */
@Component
@RequiredArgsConstructor
public class NotificationSelfListFilterOptions implements ListFilterOptionsSource {

    private final NotificationSelfService notificationSelfService;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        List<ListFilterOptionDto> typeOptions =
                notificationSelfService.listDistinctTypesForUser().stream()
                        .map(this::toTypeOption)
                        .toList();
        return new ListFilterOptionsDto(Map.of("type", typeOptions));
    }

    private ListFilterOptionDto toTypeOption(NotificationType type) {
        String label =
                messageSource.getMessage(
                        "notification.list.type." + type.name(), null, type.name(), Locale.GERMAN);
        return new ListFilterOptionDto(type.name(), label);
    }
}
