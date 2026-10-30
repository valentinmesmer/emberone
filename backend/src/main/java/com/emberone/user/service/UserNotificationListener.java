/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.platform.app.EmberOneAppProperties;
import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.mail.service.MailTemplateService;
import com.emberone.user.event.NotificationEmailChangeRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class UserNotificationListener {

    private final MailTemplateService mailTemplateService;
    private final TenantLocaleService tenantLocaleService;
    private final EmberOneAppProperties appProperties;

    @AfterCommitCallback
    public void onNotificationEmailChangeRequested(NotificationEmailChangeRequestedEvent event) {
        mailTemplateService.sendNotificationEmailVerificationIfConfigured(
                event.pendingEmail(),
                event.firstName(),
                buildVerificationUrl(event.rawToken(), event.tenantId()),
                tenantLocaleService.resolveTenantLocale(event.tenantId()));
    }

    private String buildVerificationUrl(String rawToken, Long tenantId) {
        String base = appProperties.getPublicBaseUrl();
        UriComponentsBuilder builder;
        if (!StringUtils.hasText(base)) {
            builder = UriComponentsBuilder.fromPath("/verify-notification-email");
        } else {
            builder =
                    UriComponentsBuilder.fromUriString(base.trim().replaceAll("/+$", ""))
                            .path("/verify-notification-email");
        }
        return builder.queryParam("token", rawToken)
                .queryParam("tenantId", tenantId)
                .build()
                .toUriString();
    }
}
