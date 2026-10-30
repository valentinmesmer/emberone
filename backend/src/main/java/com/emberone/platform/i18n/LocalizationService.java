/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LocalizationService {

    private final MessageSource messageSource;

    static String interpolate(String template, Map<String, Object> params) {
        if (!StringUtils.hasText(template) || params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result =
                        result.replace(
                                "{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    public String getMessage(String key, SupportedLocale locale) {
        return messageSource.getMessage(key, null, locale.toJavaLocale());
    }

    public String getMessageOrNull(String key, SupportedLocale locale) {
        try {
            String value = messageSource.getMessage(key, null, locale.toJavaLocale());
            return StringUtils.hasText(value) ? value : null;
        } catch (NoSuchMessageException ex) {
            return null;
        }
    }

    public String getInterpolatedMessage(
            String key, Map<String, Object> params, SupportedLocale locale) {
        String template = getMessageOrNull(key, locale);
        if (!StringUtils.hasText(template)) {
            return key;
        }
        return interpolate(template, params);
    }
}
