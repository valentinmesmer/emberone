/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.app.EmberOneAppProperties;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.mail.service.MailTemplateService;
import com.emberone.user.event.NotificationEmailChangeRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNotificationListenerTest {

    @Mock MailTemplateService mailTemplateService;
    @Mock TenantLocaleService tenantLocaleService;
    @Mock EmberOneAppProperties appProperties;

    UserNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener =
                new UserNotificationListener(
                        mailTemplateService, tenantLocaleService, appProperties);
    }

    @Test
    void onNotificationEmailChangeRequestedSendsVerificationMail() {
        when(appProperties.getPublicBaseUrl()).thenReturn("https://app.emberone.test/");
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);

        listener.onNotificationEmailChangeRequested(
                new NotificationEmailChangeRequestedEvent(
                        7L, 1L, "Ada", "new@example.com", "raw-token"));

        verify(mailTemplateService)
                .sendNotificationEmailVerificationIfConfigured(
                        "new@example.com",
                        "Ada",
                        "https://app.emberone.test/verify-notification-email?token=raw-token&tenantId=7",
                        SupportedLocale.DE);
    }
}
