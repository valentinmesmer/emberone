/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.type.NotificationTypeDefinition;
import com.emberone.notification.type.NotificationTypeDefinitionRegistry;
import com.emberone.platform.i18n.LocalizationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class NotificationMessageResolver implements NotificationTextResolver {

    private final LocalizationService localizationService;
    private final NotificationDisplaySupport displaySupport;
    private final NotificationTypeDefinitionRegistry typeDefinitions;

    @Override
    public String resolveTitle(Notification notification, SupportedLocale locale) {
        return resolveMessage(
                notification,
                "title",
                buildDisplayParams(notification.getType(), notification.getParams(), locale),
                locale);
    }

    @Override
    public String resolveBody(Notification notification, SupportedLocale locale) {
        return resolveMessage(
                notification,
                "body",
                buildDisplayParams(notification.getType(), notification.getParams(), locale),
                locale);
    }

    public String resolveCommentsLine(java.util.List<String> comments, SupportedLocale locale) {
        return displaySupport.resolveCommentsLine(comments, locale);
    }

    public String formatDisplayNameList(java.util.List<String> names, SupportedLocale locale) {
        return displaySupport.formatDisplayNameList(names, locale);
    }

    public String resolveApproverSuffix(String formattedNames, SupportedLocale locale) {
        return displaySupport.resolveApproverSuffix(formattedNames, locale);
    }

    public String resolveWorkflowTypeLabel(String workflowTypeKey, SupportedLocale locale) {
        return displaySupport.resolveWorkflowTypeLabel(workflowTypeKey, locale);
    }

    private Map<String, Object> buildDisplayParams(
            NotificationType type, Map<String, Object> rawParams, SupportedLocale locale) {
        if (rawParams == null || rawParams.isEmpty()) {
            return Map.of();
        }

        NotificationTypeDefinition definition = typeDefinitions.require(type);
        Set<String> derivedSourceKeys = Set.copyOf(definition.derivedSourceKeys());
        Map<String, Object> display = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawParams.entrySet()) {
            String key = entry.getKey();
            if (derivedSourceKeys.contains(key) || NotificationParamKeys.isDerivedDisplayKey(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                display.put(key, value);
            }
        }

        definition.enrichDisplayParams(rawParams, display, locale);
        return display;
    }

    private String resolveMessage(
            Notification notification,
            String part,
            Map<String, Object> params,
            SupportedLocale locale) {
        NotificationTypeDefinition definition = typeDefinitions.require(notification.getType());
        String key = definition.messageKeyPrefix(notification) + "." + part;
        String template = localizationService.getMessageOrNull(key, locale);
        if (!StringUtils.hasText(template)) {
            return notification.getType().name();
        }
        return localizationService.getInterpolatedMessage(key, params, locale);
    }
}
