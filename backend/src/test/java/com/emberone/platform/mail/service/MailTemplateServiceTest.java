/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.app.EmberOneAppProperties;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import jakarta.mail.MessagingException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class MailTemplateServiceTest {

    private static final String WELCOME_SUBJECT = "Willkommen bei EmberOne - Ihre Zugangsdaten";
    private static final String VERIFICATION_SUBJECT =
            "Bitte bestätige deine neue Benachrichtigungs-E-Mail";

    @Mock private ObjectProvider<MailService> mailServiceProvider;

    @Mock private EmberOneAppProperties appProperties;

    @Mock private MailService mailService;

    @Mock private MailMessageResolver mailMessageResolver;

    @InjectMocks private MailTemplateService mailTemplateService;

    @Test
    void sendWelcomeNewUserIfConfigured_trimsTrailingSlashesFromLoginUrl()
            throws MessagingException {
        when(mailServiceProvider.getIfAvailable()).thenReturn(mailService);
        when(appProperties.getPublicBaseUrl()).thenReturn("  https://app.emberone.example/  ");
        when(mailMessageResolver.welcomeVariables(
                        eq(SupportedLocale.DE),
                        eq("Max"),
                        eq("max.mustermann"),
                        eq("temp-pass"),
                        eq("https://app.emberone.example")))
                .thenReturn(
                        Map.of("loginUrl", "https://app.emberone.example", "hasLoginUrl", true));
        when(mailMessageResolver.welcomeSubject(SupportedLocale.DE)).thenReturn(WELCOME_SUBJECT);

        mailTemplateService.sendWelcomeNewUserIfConfigured(
                "user@example.com", "Max", "max.mustermann", "temp-pass", SupportedLocale.DE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mailService)
                .sendTemplateMail(
                        eq("user@example.com"),
                        eq(WELCOME_SUBJECT),
                        eq("welcome-new-user"),
                        variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        assertThat(variables.get("loginUrl")).isEqualTo("https://app.emberone.example");
        assertThat(variables.get("hasLoginUrl")).isEqualTo(true);
    }

    @Test
    void sendWelcomeNewUserIfConfigured_skipsWhenMailNotConfigured() throws MessagingException {
        when(mailServiceProvider.getIfAvailable()).thenReturn(null);

        mailTemplateService.sendWelcomeNewUserIfConfigured(
                "user@example.com", "Max", "max.mustermann", "temp-pass", SupportedLocale.DE);

        verify(mailService, never()).sendTemplateMail(any(), any(), any(), any());
    }

    @Test
    void sendWelcomeNewUserIfConfigured_setsEmptyLoginUrlWhenBaseMissing()
            throws MessagingException {
        when(mailServiceProvider.getIfAvailable()).thenReturn(mailService);
        when(appProperties.getPublicBaseUrl()).thenReturn("  ");
        when(mailMessageResolver.welcomeVariables(
                        eq(SupportedLocale.DE),
                        eq(null),
                        eq("max.mustermann"),
                        eq("temp-pass"),
                        eq("")))
                .thenReturn(Map.of("loginUrl", "", "hasLoginUrl", false));
        when(mailMessageResolver.welcomeSubject(SupportedLocale.DE)).thenReturn(WELCOME_SUBJECT);

        mailTemplateService.sendWelcomeNewUserIfConfigured(
                "user@example.com", null, "max.mustermann", "temp-pass", SupportedLocale.DE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendTemplateMail(any(), any(), any(), variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        assertThat(variables.get("loginUrl")).isEqualTo("");
        assertThat(variables.get("hasLoginUrl")).isEqualTo(false);
    }

    @Test
    void sendNotificationEmailVerificationBuildsTemplateVariables() throws MessagingException {
        when(mailServiceProvider.getIfAvailable()).thenReturn(mailService);
        when(appProperties.getPublicBaseUrl()).thenReturn("https://app.example/");
        when(mailMessageResolver.notificationEmailVerificationVariables(
                        eq(SupportedLocale.DE),
                        eq("Ada"),
                        eq("new@example.com"),
                        eq("https://app.example/verify"),
                        eq("https://app.example")))
                .thenReturn(
                        Map.of(
                                "pendingEmail", "new@example.com",
                                "verificationUrl", "https://app.example/verify",
                                "recipientFirstName", "Ada"));
        when(mailMessageResolver.notificationEmailVerificationSubject(SupportedLocale.DE))
                .thenReturn(VERIFICATION_SUBJECT);

        mailTemplateService.sendNotificationEmailVerificationIfConfigured(
                " new@example.com ", "Ada", "https://app.example/verify", SupportedLocale.DE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService)
                .sendTemplateMail(
                        eq("new@example.com"),
                        eq(VERIFICATION_SUBJECT),
                        eq("notification-email-verification"),
                        captor.capture());
        assertThat(captor.getValue().get("pendingEmail")).isEqualTo("new@example.com");
        assertThat(captor.getValue().get("verificationUrl"))
                .isEqualTo("https://app.example/verify");
    }

    @Test
    void invalidInputsAndMessagingFailuresAreHandled() throws MessagingException {
        when(mailServiceProvider.getIfAvailable()).thenReturn(mailService);

        mailTemplateService.sendWelcomeNewUserIfConfigured(
                " ", "Max", "user", "password", SupportedLocale.DE);
        mailTemplateService.sendWelcomeNewUserIfConfigured(
                "user@example.com", "Max", "user", " ", SupportedLocale.DE);
        mailTemplateService.sendNotificationEmailVerificationIfConfigured(
                " ", "Ada", "https://x", SupportedLocale.DE);
        verify(mailService, never()).sendTemplateMail(any(), any(), any(), any());

        when(appProperties.getPublicBaseUrl()).thenReturn("");
        when(mailMessageResolver.welcomeVariables(any(), any(), any(), any(), any()))
                .thenReturn(Map.of("loginUrl", "", "hasLoginUrl", false));
        when(mailMessageResolver.welcomeSubject(SupportedLocale.DE)).thenReturn(WELCOME_SUBJECT);
        doThrow(new MessagingException("smtp down"))
                .when(mailService)
                .sendTemplateMail(any(), any(), any(), any());
        mailTemplateService.sendWelcomeNewUserIfConfigured(
                "user@example.com", "Max", "user", "password", SupportedLocale.DE);
        verify(mailService).sendTemplateMail(any(), any(), any(), any());
    }
}
