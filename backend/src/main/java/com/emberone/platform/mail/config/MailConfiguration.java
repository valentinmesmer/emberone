/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.mail.config;

import com.emberone.platform.mail.service.MailService;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
@Conditional(EmberOneMailEnabledCondition.class)
public class MailConfiguration {

    @Bean
    public JavaMailSender javaMailSender(EmberOneMailProperties mailProperties) {
        EmberOneMailProperties.Smtp smtp = mailProperties.getSmtp();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        sender.setUsername(smtp.getUsername());
        sender.setPassword(smtp.getPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", Boolean.toString(smtp.isAuth()));
        props.put("mail.smtp.starttls.enable", Boolean.toString(smtp.isStartTls()));
        props.put("mail.smtp.starttls.required", Boolean.toString(smtp.isStartTls()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        sender.setDefaultEncoding("UTF-8");
        return sender;
    }

    @Bean(name = "emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/mail/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setCheckExistence(true);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Bean
    public MailService mailService(
            JavaMailSender javaMailSender,
            SpringTemplateEngine emailTemplateEngine,
            EmberOneMailProperties emberoneMailProperties) {
        return new MailService(javaMailSender, emailTemplateEngine, emberoneMailProperties);
    }
}
