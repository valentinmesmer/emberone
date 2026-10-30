/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.identity.event.UserWelcomeNeededEvent;
import com.emberone.identity.event.UsernameChangedEvent;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import com.emberone.platform.mail.service.MailTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityNotificationListenerTest {

    @Mock NotificationService notificationService;
    @Mock MailTemplateService mailTemplateService;
    @Mock TenantLocaleService tenantLocaleService;
    @Mock MailMessageResolver mailMessageResolver;

    @InjectMocks IdentityNotificationListener listener;

    @Test
    void onUserWelcomeNeededSendsWelcomeMail() {
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);

        listener.onUserWelcomeNeeded(
                new UserWelcomeNeededEvent(
                        7L, 1L, "ada@example.com", "Ada", "ada", "Temp123Password"));

        verify(mailTemplateService)
                .sendWelcomeNewUserIfConfigured(
                        "ada@example.com", "Ada", "ada", "Temp123Password", SupportedLocale.DE);
    }

    @Test
    void onUsernameChangedSendsNotificationWithPreviousUsername() {
        listener.onUsernameChanged(new UsernameChangedEvent(7L, 1L, "ada", "ada.new"));

        NotificationRequest request = captureNotify();
        assertThat(request.type()).isEqualTo(NotificationType.USERNAME_CHANGED);
        assertThat(request.params())
                .containsEntry(NotificationParamKeys.PREVIOUS_USERNAME, "ada")
                .containsEntry(NotificationParamKeys.NEW_USERNAME, "ada.new");
    }

    @Test
    void onUsernameChangedFallsBackToLocalizedTextWhenNoPreviousUsername() {
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(mailMessageResolver.emptyPreviousUsername(SupportedLocale.DE))
                .thenReturn("keinen bisherigen Benutzernamen");

        listener.onUsernameChanged(new UsernameChangedEvent(7L, 1L, "", "ada.new"));

        NotificationRequest request = captureNotify();
        assertThat(request.params())
                .containsEntry(
                        NotificationParamKeys.PREVIOUS_USERNAME, "keinen bisherigen Benutzernamen");
    }

    private NotificationRequest captureNotify() {
        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        return captor.getValue();
    }
}
