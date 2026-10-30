/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoType;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TodoMessageResolver {

    private final LocalizationService localizationService;
    private final NotificationDisplaySupport displaySupport;

    public String resolveTitle(Todo todo, SupportedLocale locale) {
        return resolveMessage(todo.getType(), "title", buildDisplayParams(todo, locale), locale);
    }

    public String resolveBody(Todo todo, SupportedLocale locale) {
        return resolveMessage(todo.getType(), "body", buildDisplayParams(todo, locale), locale);
    }

    private Map<String, Object> buildDisplayParams(Todo todo, SupportedLocale locale) {
        Map<String, Object> params = new HashMap<>();
        if (todo.getParams() != null) {
            params.putAll(todo.getParams());
        }
        Object workflowTypeKey = params.get(TodoParamKeys.WORKFLOW_TYPE_KEY);
        if (workflowTypeKey instanceof String key && StringUtils.hasText(key)) {
            params.put(
                    TodoParamKeys.WORKFLOW_TYPE_LABEL,
                    displaySupport.resolveWorkflowTypeLabel(key, locale));
        }
        Object certificationStatus = params.get(TodoParamKeys.STATUS);
        if (certificationStatus instanceof String statusKey && StringUtils.hasText(statusKey)) {
            String label = displaySupport.resolveCertificationStatusLabel(statusKey, locale);
            if (StringUtils.hasText(label)) {
                params.put(TodoParamKeys.STATUS_LABEL, label);
            }
        }
        return params;
    }

    private String resolveMessage(
            TodoType type, String suffix, Map<String, Object> params, SupportedLocale locale) {
        String key = "todo." + type.name() + "." + suffix;
        String template = localizationService.getMessageOrNull(key, locale);
        if (!StringUtils.hasText(template)) {
            return type.name();
        }
        return localizationService.getInterpolatedMessage(key, params, locale);
    }
}
