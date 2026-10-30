/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.i18n.NotificationMessageResolver;
import com.emberone.platform.i18n.TenantLocaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDtoAssemblerTest {

    @Mock NotificationMapper notificationMapper;
    @Mock NotificationMessageResolver messageResolver;
    @Mock TenantLocaleService tenantLocaleService;

    @InjectMocks NotificationDtoAssembler assembler;

    @Test
    void toDtoResolvesDisplayTextWithoutPersistingIt() {
        Notification notification =
                Notification.builder()
                        .id(5L)
                        .tenantId(7L)
                        .type(NotificationType.WORKFLOW_APPROVED)
                        .build();
        NotificationDto mapped = NotificationDto.builder().id(5L).type("WORKFLOW_APPROVED").build();

        when(notificationMapper.toDto(notification)).thenReturn(mapped);
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(messageResolver.resolveTitle(notification, SupportedLocale.DE))
                .thenReturn("Ein Antrag Namensänderung wurde genehmigt");
        when(messageResolver.resolveBody(notification, SupportedLocale.DE))
                .thenReturn("Dein Antrag auf Namensänderung wurde genehmigt.");

        NotificationDto result = assembler.toDto(notification);

        assertThat(result.getTitle()).isEqualTo("Ein Antrag Namensänderung wurde genehmigt");
        assertThat(result.getBody()).isEqualTo("Dein Antrag auf Namensänderung wurde genehmigt.");
        verify(notificationMapper).toDto(notification);
    }
}
