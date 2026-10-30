/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.mapper;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.dto.NotificationPageDto;
import com.emberone.notification.i18n.NotificationMessageResolver;
import com.emberone.platform.i18n.TenantLocaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDtoAssembler {

    private final NotificationMapper notificationMapper;
    private final NotificationMessageResolver messageResolver;
    private final TenantLocaleService tenantLocaleService;

    public NotificationDto toDto(Notification notification) {
        NotificationDto dto = notificationMapper.toDto(notification);
        SupportedLocale locale =
                tenantLocaleService.resolveTenantLocale(notification.getTenantId());
        dto.setTitle(messageResolver.resolveTitle(notification, locale));
        dto.setBody(messageResolver.resolveBody(notification, locale));
        return dto;
    }

    public NotificationPageDto toPageDto(Page<Notification> page) {
        return NotificationPageDto.builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
