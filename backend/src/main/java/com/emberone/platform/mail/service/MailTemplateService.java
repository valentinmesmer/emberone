/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.service;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.app.EmberOneAppProperties;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import jakarta.mail.MessagingException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * High-level entry for application e-mails: prepares Thymeleaf template variables, picks subject
 * and template name, and delegates sending to {@link MailService} when SMTP is configured ({@code
 * emberone.mail.smtp.host}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailTemplateService {

    private static final String TEMPLATE_WELCOME = "welcome-new-user";
    private static final String TEMPLATE_EMAIL_VERIFICATION = "notification-email-verification";

    private final ObjectProvider<MailService> mailServiceProvider;
    private final EmberOneAppProperties appProperties;
    private final MailMessageResolver mailMessageResolver;

    private static String buildLoginUrl(String rawBase) {
        if (!StringUtils.hasText(rawBase)) {
            return "";
        }
        return rawBase.trim().replaceAll("/+$", "");
    }

    public void sendWelcomeNewUserIfConfigured(
            String privateEmail,
            String firstName,
            String username,
            String temporaryPassword,
            SupportedLocale locale) {
        MailService mailService = mailServiceProvider.getIfAvailable();
        if (mailService == null) {
            log.debug("Mail is not configured; skipping welcome email for user {}", username);
            return;
        }
        if (!StringUtils.hasText(privateEmail)) {
            log.warn("No private email on file; cannot send welcome email for user {}", username);
            return;
        }
        if (!StringUtils.hasText(temporaryPassword)) {
            log.warn("Missing temporary password; cannot send welcome email for user {}", username);
            return;
        }

        String loginUrl = buildLoginUrl(appProperties.getPublicBaseUrl());
        Map<String, Object> variables =
                mailMessageResolver.welcomeVariables(
                        locale, firstName, username, temporaryPassword, loginUrl);

        try {
            mailService.sendTemplateMail(
                    privateEmail.trim(),
                    mailMessageResolver.welcomeSubject(locale),
                    TEMPLATE_WELCOME,
                    variables);
            log.debug("Sent welcome email for user {}", username);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email for user {}", username, e);
        }
    }

    public void sendNotificationEmailVerificationIfConfigured(
            String pendingEmail, String firstName, String verificationUrl, SupportedLocale locale) {
        MailService mailService = mailServiceProvider.getIfAvailable();
        if (mailService == null) {
            log.debug("Mail is not configured; skipping notification-email verification");
            return;
        }
        if (!StringUtils.hasText(pendingEmail)) {
            log.warn("No pending email; cannot send notification-email verification");
            return;
        }

        String loginUrl = buildLoginUrl(appProperties.getPublicBaseUrl());
        Map<String, Object> variables =
                mailMessageResolver.notificationEmailVerificationVariables(
                        locale, firstName, pendingEmail.trim(), verificationUrl, loginUrl);

        try {
            mailService.sendTemplateMail(
                    pendingEmail.trim(),
                    mailMessageResolver.notificationEmailVerificationSubject(locale),
                    TEMPLATE_EMAIL_VERIFICATION,
                    variables);
            log.debug("Sent notification-email verification");
        } catch (MessagingException e) {
            log.error("Failed to send notification-email verification", e);
        }
    }
}
