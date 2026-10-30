/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.channel;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.channel.email.EmailRenderContext;
import com.emberone.notification.channel.email.EmailRenderResult;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.i18n.NotificationMessageResolver;
import com.emberone.notification.type.NotificationTypeDefinitionRegistry;
import com.emberone.platform.app.EmberOneAppProperties;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.mail.service.MailService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserContactInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelSender implements ChannelSender {

    private final ObjectProvider<MailService> mailServiceProvider;
    private final UserContactInformationService contactInformationService;
    private final UserRepository userRepository;
    private final TenantLocaleService tenantLocaleService;
    private final EmberOneAppProperties appProperties;
    private final NotificationTypeDefinitionRegistry typeDefinitions;
    private final NotificationMessageResolver messageResolver;

    private static String buildLoginUrl(String rawBase) {
        if (!StringUtils.hasText(rawBase)) {
            return "";
        }
        return rawBase.trim().replaceAll("/+$", "");
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) throws Exception {
        MailService mailService = mailServiceProvider.getIfAvailable();
        if (mailService == null) {
            throw new RecipientNotReachableException("Mail is not configured");
        }

        String email =
                contactInformationService
                        .findPrivateEmail(notification.getRecipientUserId())
                        .orElse(null);
        if (email == null) {
            log.warn(
                    "No email address for user {}; skipping email delivery for notification {}",
                    notification.getRecipientUserId(),
                    notification.getId());
            throw new RecipientNotReachableException("No email address on file");
        }

        SupportedLocale locale =
                tenantLocaleService.resolveTenantLocale(notification.getTenantId());
        User user = userRepository.findById(notification.getRecipientUserId()).orElse(null);
        String firstName = user != null ? user.getFirstName() : "";
        String loginUrl = buildLoginUrl(appProperties.getPublicBaseUrl());

        EmailRenderContext context =
                new EmailRenderContext(notification, locale, firstName, loginUrl, messageResolver);
        EmailRenderResult rendered =
                typeDefinitions.require(notification.getType()).renderEmail(context);

        mailService.sendTemplateMail(
                email.trim(), rendered.subject(), rendered.templateName(), rendered.variables());
    }
}
