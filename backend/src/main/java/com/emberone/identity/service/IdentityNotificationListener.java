/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import com.emberone.identity.event.UserWelcomeNeededEvent;
import com.emberone.identity.event.UsernameChangedEvent;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import com.emberone.platform.mail.service.MailTemplateService;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sends the welcome mail and username-change notification for identity events; kept separate from
 * {@link IdentityUserService} so that service stays focused on identity provisioning.
 */
@Service
@RequiredArgsConstructor
public class IdentityNotificationListener {

    private final NotificationService notificationService;
    private final MailTemplateService mailTemplateService;
    private final TenantLocaleService tenantLocaleService;
    private final MailMessageResolver mailMessageResolver;

    @AfterCommitCallback
    public void onUserWelcomeNeeded(UserWelcomeNeededEvent event) {
        mailTemplateService.sendWelcomeNewUserIfConfigured(
                event.privateEmail(),
                event.firstName(),
                event.username(),
                event.temporaryPassword(),
                tenantLocaleService.resolveTenantLocale(event.tenantId()));
    }

    @AfterCommitEventListener
    public void onUsernameChanged(UsernameChangedEvent event) {
        String emptyPrevious =
                mailMessageResolver.emptyPreviousUsername(
                        tenantLocaleService.resolveTenantLocale(event.tenantId()));
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(event.tenantId())
                        .recipientUserIds(Set.of(event.userId()))
                        .type(NotificationType.USERNAME_CHANGED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.PREVIOUS_USERNAME,
                                        event.previousUsername().isEmpty()
                                                ? emptyPrevious
                                                : event.previousUsername(),
                                        NotificationParamKeys.NEW_USERNAME,
                                        event.newUsername()))
                        .build());
    }
}
