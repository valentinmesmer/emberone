/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.i18n.LocalizationService;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MailMessageResolver {

    private final LocalizationService localizationService;

    public Map<String, Object> welcomeVariables(
            SupportedLocale locale,
            String firstName,
            String username,
            String temporaryPassword,
            String loginUrl) {
        Map<String, Object> variables = commonVariables(locale, loginUrl);
        String trimmedFirstName = StringUtils.hasText(firstName) ? firstName.trim() : "";
        variables.put("username", username);
        variables.put("temporaryPassword", temporaryPassword);
        variables.put("pageTitle", message("mail.welcome-new-user.pageTitle", locale));
        variables.put(
                "heading",
                StringUtils.hasText(trimmedFirstName)
                        ? message(
                                "mail.welcome-new-user.headingWithName",
                                Map.of("name", trimmedFirstName),
                                locale)
                        : message("mail.welcome-new-user.heading", locale));
        variables.put("intro", message("mail.welcome-new-user.intro", locale));
        variables.put("usernameLabel", message("mail.welcome-new-user.usernameLabel", locale));
        variables.put(
                "temporaryPasswordLabel",
                message("mail.welcome-new-user.temporaryPasswordLabel", locale));
        variables.put("passwordHint", message("mail.welcome-new-user.passwordHint", locale));
        return variables;
    }

    public Map<String, Object> usernameChangedVariables(
            SupportedLocale locale,
            String firstName,
            String previousUsername,
            String newUsername,
            String loginUrl) {
        Map<String, Object> variables = commonVariables(locale, loginUrl);
        String trimmedFirstName = StringUtils.hasText(firstName) ? firstName.trim() : "";
        variables.put("previousUsername", previousUsername);
        variables.put("newUsername", newUsername);
        variables.put("pageTitle", message("mail.username-changed.pageTitle", locale));
        variables.put("heading", message("mail.username-changed.heading", locale));
        variables.put(
                "greeting",
                StringUtils.hasText(trimmedFirstName)
                        ? message(
                                "mail.username-changed.greetingWithName",
                                Map.of("name", trimmedFirstName),
                                locale)
                        : message("mail.username-changed.greeting", locale));
        variables.put("intro", message("mail.username-changed.intro", locale));
        variables.put(
                "previousUsernameLabel",
                message("mail.username-changed.previousUsernameLabel", locale));
        variables.put(
                "newUsernameLabel", message("mail.username-changed.newUsernameLabel", locale));
        variables.put("passwordHint", message("mail.username-changed.passwordHint", locale));
        variables.put("noLoginUrlHint", message("mail.username-changed.noLoginUrlHint", locale));
        return variables;
    }

    public Map<String, Object> notificationEmailVerificationVariables(
            SupportedLocale locale,
            String firstName,
            String pendingEmail,
            String verificationUrl,
            String loginUrl) {
        Map<String, Object> variables = commonVariables(locale, loginUrl);
        String trimmedFirstName = StringUtils.hasText(firstName) ? firstName.trim() : "";
        variables.put("pendingEmail", pendingEmail);
        variables.put("verificationUrl", verificationUrl);
        variables.put("hasVerificationUrl", StringUtils.hasText(verificationUrl));
        variables.put(
                "pageTitle", message("mail.notification-email-verification.pageTitle", locale));
        variables.put("heading", message("mail.notification-email-verification.heading", locale));
        variables.put(
                "greeting",
                StringUtils.hasText(trimmedFirstName)
                        ? message(
                                "mail.notification-email-verification.greetingWithName",
                                Map.of("name", trimmedFirstName),
                                locale)
                        : message("mail.notification-email-verification.greeting", locale));
        variables.put("intro", message("mail.notification-email-verification.intro", locale));
        variables.put(
                "emailLabel", message("mail.notification-email-verification.emailLabel", locale));
        variables.put(
                "confirmButton",
                message("mail.notification-email-verification.confirmButton", locale));
        variables.put(
                "expiryHint", message("mail.notification-email-verification.expiryHint", locale));
        variables.put(
                "noVerificationUrlHint",
                message("mail.notification-email-verification.noVerificationUrlHint", locale));
        return variables;
    }

    public Map<String, Object> notificationVariables(
            SupportedLocale locale, String title, String body, String loginUrl) {
        Map<String, Object> variables = commonVariables(locale, loginUrl);
        variables.put("title", title);
        variables.put("body", body);
        variables.put("pageTitle", title);
        variables.put("heading", title);
        variables.put("messageLabel", message("mail.notification.messageLabel", locale));
        variables.put("openButton", message("mail.notification.openButton", locale));
        variables.put("noLoginUrlHint", message("mail.notification.noLoginUrlHint", locale));
        return variables;
    }

    public String welcomeSubject(SupportedLocale locale) {
        return message("mail.welcome-new-user.subject", locale);
    }

    public String notificationEmailVerificationSubject(SupportedLocale locale) {
        return message("mail.notification-email-verification.subject", locale);
    }

    public String emptyPreviousUsername(SupportedLocale locale) {
        return message("mail.username-changed.emptyPrevious", locale);
    }

    private Map<String, Object> commonVariables(SupportedLocale locale, String loginUrl) {
        String normalizedLoginUrl = StringUtils.hasText(loginUrl) ? loginUrl : "";
        Map<String, Object> variables = new HashMap<>();
        variables.put("loginUrl", normalizedLoginUrl);
        variables.put("hasLoginUrl", StringUtils.hasText(normalizedLoginUrl));
        variables.put("loginButton", message("mail.common.loginButton", locale));
        variables.put("loginUrlHint", message("mail.common.loginUrlHint", locale));
        variables.put("noLoginUrlHint", message("mail.common.noLoginUrlHint", locale));
        variables.put("footer", message("mail.common.footer", locale));
        variables.put(
                "copyright",
                message("mail.common.copyright", Map.of("year", Year.now().getValue()), locale));
        return variables;
    }

    private String message(String key, SupportedLocale locale) {
        return localizationService.getMessage(key, locale);
    }

    private String message(String key, Map<String, Object> params, SupportedLocale locale) {
        return localizationService.getInterpolatedMessage(key, params, locale);
    }
}
