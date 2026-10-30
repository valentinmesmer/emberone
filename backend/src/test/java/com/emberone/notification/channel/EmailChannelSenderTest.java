/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.channel.email.EmailRenderContext;
import com.emberone.notification.channel.email.EmailRenderResult;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationMessageResolver;
import com.emberone.notification.type.NotificationTypeDefinition;
import com.emberone.notification.type.NotificationTypeDefinitionRegistry;
import com.emberone.platform.app.EmberOneAppProperties;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.mail.service.MailService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserContactInformationService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class EmailChannelSenderTest {

    private static final String USERNAME_CHANGED_SUBJECT =
            "EmberOne - Ihre Benutzerkennung wurde geändert";

    @Mock ObjectProvider<MailService> mailServiceProvider;
    @Mock MailService mailService;
    @Mock UserContactInformationService contactInformationService;
    @Mock UserRepository userRepository;
    @Mock TenantLocaleService tenantLocaleService;
    @Mock EmberOneAppProperties appProperties;
    @Mock NotificationTypeDefinitionRegistry typeDefinitions;
    @Mock NotificationMessageResolver messageResolver;
    @Mock NotificationTypeDefinition usernameChangedDefinition;
    @Mock NotificationTypeDefinition workflowOpenedDefinition;

    EmailChannelSender sender;

    private static Notification notification(NotificationType type, Map<String, Object> params) {
        return Notification.builder()
                .id(1L)
                .tenantId(7L)
                .recipientUserId(5L)
                .type(type)
                .params(params)
                .build();
    }

    private static NotificationDelivery delivery() {
        return NotificationDelivery.builder().channel(NotificationChannel.EMAIL).build();
    }

    @BeforeEach
    void setUp() {
        sender =
                new EmailChannelSender(
                        mailServiceProvider,
                        contactInformationService,
                        userRepository,
                        tenantLocaleService,
                        appProperties,
                        typeDefinitions,
                        messageResolver);
    }

    @Test
    void sendsUsernameChangedMailThroughTypeDefinition() throws Exception {
        Notification notification =
                notification(
                        NotificationType.USERNAME_CHANGED,
                        Map.of(
                                "previousUsername", "ada",
                                "newUsername", "ada.new"));
        stubCommonMailSetup();
        when(typeDefinitions.require(NotificationType.USERNAME_CHANGED))
                .thenReturn(usernameChangedDefinition);
        when(usernameChangedDefinition.renderEmail(any(EmailRenderContext.class)))
                .thenReturn(
                        new EmailRenderResult(
                                USERNAME_CHANGED_SUBJECT,
                                "username-changed",
                                Map.of(
                                        "previousUsername", "ada",
                                        "newUsername", "ada.new",
                                        "recipientFirstName", "Ada")));

        sender.send(notification, delivery());

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.captor();
        verify(mailService)
                .sendTemplateMail(
                        eq("ada@example.com"),
                        eq(USERNAME_CHANGED_SUBJECT),
                        eq("username-changed"),
                        variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("previousUsername", "ada")
                .containsEntry("newUsername", "ada.new")
                .containsEntry("recipientFirstName", "Ada");
    }

    @Test
    void sendsGenericNotificationThroughDefaultRendering() throws Exception {
        Notification notification =
                notification(NotificationType.WORKFLOW_OPENED, Map.of("workflowId", 5));
        stubCommonMailSetup();
        when(typeDefinitions.require(NotificationType.WORKFLOW_OPENED))
                .thenReturn(workflowOpenedDefinition);
        when(workflowOpenedDefinition.renderEmail(any(EmailRenderContext.class)))
                .thenReturn(
                        new EmailRenderResult(
                                "Neuer Workflow",
                                "notification",
                                Map.of(
                                        "title",
                                        "Neuer Workflow",
                                        "body",
                                        "Ein Workflow wurde für Sie geöffnet.")));

        sender.send(notification, delivery());

        verify(mailService)
                .sendTemplateMail(
                        eq("ada@example.com"), eq("Neuer Workflow"), eq("notification"), any());
    }

    private void stubCommonMailSetup() {
        when(mailServiceProvider.getIfAvailable()).thenReturn(mailService);
        when(contactInformationService.findPrivateEmail(5L))
                .thenReturn(Optional.of("ada@example.com"));
        when(userRepository.findById(5L))
                .thenReturn(Optional.of(User.builder().id(5L).firstName("Ada").build()));
        when(tenantLocaleService.resolveTenantLocale(7L)).thenReturn(SupportedLocale.DE);
        when(appProperties.getPublicBaseUrl()).thenReturn("https://app.emberone.test");
    }
}
