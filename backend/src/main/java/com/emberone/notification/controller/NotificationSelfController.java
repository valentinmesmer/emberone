/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.controller;

import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.dto.NotificationPageDto;
import com.emberone.notification.dto.UnreadNotificationCountDto;
import com.emberone.notification.list.NotificationListFilter;
import com.emberone.notification.list.NotificationSelfListFilterOptions;
import com.emberone.notification.service.NotificationSelfService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/self/notifications")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class NotificationSelfController {

    private final NotificationSelfService notificationSelfService;
    private final NotificationSelfListFilterOptions selfListFilterOptions;

    @GetMapping("/list-filter-options")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(selfListFilterOptions.filterOptions());
    }

    @GetMapping
    public ResponseEntity<NotificationPageDto> listNotifications(
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        return ResponseEntity.ok(
                notificationSelfService.listNotifications(
                        query, NotificationListFilter.parse(status, type)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountDto> getUnreadCount() {
        return ResponseEntity.ok(notificationSelfService.getUnreadCount());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationSelfService.getNotification(id));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationSelfService.markRead(id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationSelfService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
