/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.service;

import com.emberone.platform.mail.config.EmberOneMailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
public class MailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine emailTemplateEngine;
    private final EmberOneMailProperties mailProperties;

    public MailService(
            JavaMailSender javaMailSender,
            @Qualifier("emailTemplateEngine") SpringTemplateEngine emailTemplateEngine,
            EmberOneMailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.emailTemplateEngine = emailTemplateEngine;
        this.mailProperties = mailProperties;
    }

    private static List<String> normalizeAddresses(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String a : raw) {
            if (StringUtils.hasText(a)) {
                out.add(a.trim());
            }
        }
        return out;
    }

    private static String stripCrlf(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n]", "");
    }

    public void sendTemplateMail(
            String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        String html = emailTemplateEngine.process(templateName, context);
        sendHtmlMail(List.of(to), subject, html);
        log.debug("Sent template mail template={}", templateName);
    }

    private void sendHtmlMail(List<String> to, String subject, String htmlBody)
            throws MessagingException {
        List<String> toNorm = normalizeAddresses(to);
        if (toNorm.isEmpty()) {
            throw new MessagingException("At least one non-blank 'to' address is required");
        }

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setTo(toNorm.toArray(new String[0]));
        helper.setSubject(stripCrlf(subject));
        helper.setText(htmlBody, true);
        setFrom(helper);
        javaMailSender.send(message);
        log.debug("Sent mail subject={} recipientCount={}", subject, toNorm.size());
    }

    private void setFrom(MimeMessageHelper helper) throws MessagingException {
        String address = mailProperties.getFromAddress();
        if (!StringUtils.hasText(address)) {
            throw new MessagingException("emberone.mail.from-address is not configured");
        }
        try {
            if (StringUtils.hasText(mailProperties.getFromName())) {
                helper.setFrom(
                        new InternetAddress(
                                address,
                                mailProperties.getFromName(),
                                StandardCharsets.UTF_8.name()));
            } else {
                helper.setFrom(address);
            }
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Invalid From encoding", e);
        }
    }
}
