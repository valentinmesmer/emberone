/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.platform.mail.config.EmberOneMailProperties;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

class MailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
    private final EmberOneMailProperties properties = new EmberOneMailProperties();
    private MailService service;

    @BeforeEach
    void setUp() {
        properties.setFromAddress("noreply@emberone.example");
        properties.setFromName("EmberOne");
        service = new MailService(mailSender, templateEngine, properties);
    }

    @Test
    void sendTemplateMailNormalizesRecipientAndBuildsHtmlMessage() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("alarm"), any(Context.class)))
                .thenReturn("<strong>Ready</strong>");

        service.sendTemplateMail(" alice@example.com ", "Alarm", "alarm", Map.of());
        message.saveChanges();

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Alarm");
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString)
                .containsExactly("alice@example.com");
        assertThat(message.getFrom())
                .extracting(Object::toString)
                .containsExactly("EmberOne <noreply@emberone.example>");
        assertThat(message.getContentType()).contains("multipart/mixed");
    }

    @Test
    void sendTemplateMailPassesVariablesAndRenderedHtml() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("welcome"), any(Context.class)))
                .thenReturn("<p>Hello Ada</p>");

        service.sendTemplateMail("ada@example.com", "Welcome", "welcome", Map.of("name", "Ada"));

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("welcome"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("name")).isEqualTo("Ada");
        verify(mailSender).send(message);
    }

    @Test
    void sendRejectsMissingRecipientsAndMissingFromAddress() {
        when(templateEngine.process(eq("welcome"), any(Context.class))).thenReturn("Body");
        assertThatThrownBy(() -> service.sendTemplateMail(" ", "Subject", "welcome", Map.of()))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("At least one");
        verify(mailSender, never()).createMimeMessage();

        properties.setFromAddress(" ");
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        assertThatThrownBy(
                        () ->
                                service.sendTemplateMail(
                                        "ada@example.com", "Subject", "welcome", Map.of()))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("from-address");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
