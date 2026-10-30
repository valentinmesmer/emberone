/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.platform.i18n.LocalizationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class NotificationDisplaySupport {

    private final LocalizationService localizationService;

    public static List<String> extractStringList(Map<String, Object> params, String listKey) {
        Object rawList = params.get(listKey);
        if (!(rawList instanceof List<?> list)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String value = String.valueOf(item).trim();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    public static List<String> extractDisplayNameList(Map<String, Object> params, String listKey) {
        return extractStringList(params, listKey);
    }

    public String resolveCommentsLine(List<String> comments, SupportedLocale locale) {
        if (comments == null || comments.isEmpty()) {
            return "";
        }
        if (comments.size() == 1) {
            return localizationService.getInterpolatedMessage(
                    "notification.commentLine", Map.of("comment", comments.getFirst()), locale);
        }
        return localizationService.getInterpolatedMessage(
                "notification.commentsLine",
                Map.of("comments", String.join("\n", comments)),
                locale);
    }

    public String formatDisplayNameList(List<String> names, SupportedLocale locale) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.getFirst();
        }
        if (names.size() == 2) {
            return localizationService.getInterpolatedMessage(
                    "notification.nameList.two",
                    Map.of("first", names.get(0), "second", names.get(1)),
                    locale);
        }
        String initial = String.join(", ", names.subList(0, names.size() - 1));
        return localizationService.getInterpolatedMessage(
                "notification.nameList.many",
                Map.of("initial", initial, "last", names.getLast()),
                locale);
    }

    public String resolveApproverSuffix(String formattedNames, SupportedLocale locale) {
        if (!StringUtils.hasText(formattedNames)) {
            return ".";
        }
        return localizationService.getInterpolatedMessage(
                "notification.approverSuffix", Map.of("names", formattedNames.trim()), locale);
    }

    public String resolveWorkflowTypeLabel(String workflowTypeKey, SupportedLocale locale) {
        String key =
                "workflow.type."
                        + (StringUtils.hasText(workflowTypeKey) ? workflowTypeKey : "UNKNOWN");
        String label = localizationService.getMessageOrNull(key, locale);
        if (StringUtils.hasText(label)) {
            return label;
        }
        return localizationService.getMessage("workflow.type.UNKNOWN", locale);
    }

    public String resolveCertificationStatusLabel(String statusKey, SupportedLocale locale) {
        if (!StringUtils.hasText(statusKey)) {
            return null;
        }
        return localizationService.getMessageOrNull("certification.status." + statusKey, locale);
    }
}
