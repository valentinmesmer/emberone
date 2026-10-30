/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.util;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * Builds stable, URL-safe {@code custom:…} config keys for standard templates.
 *
 * <p>Keys are unique per standard ({@code tenant_id + standard_id + config_key}), so different
 * tenants may use the same title and receive the same slug without conflict.
 */
public final class StandardConfigKey {

    public static final String CUSTOM_PREFIX = "custom:";

    private StandardConfigKey() {}

    /** ASCII slug derived from a human title (German umlauts transliterated). */
    private static String slugFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String normalized = transliterateGerman(title.trim().toLowerCase(Locale.ROOT));
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        return normalized;
    }

    private static String customFromTitle(String title) {
        String slug = slugFromTitle(title);
        if (slug.isEmpty()) {
            slug = "group";
        }
        return CUSTOM_PREFIX + slug;
    }

    /**
     * Stable config key for a custom template derived from its i18n message key (e.g. {@code
     * organizationGroup.custom.office} → {@code custom:office}).
     */
    public static String customFromMessageKey(String messageKey) {
        if (messageKey == null || messageKey.isBlank()) {
            throw new IllegalArgumentException("messageKey is required");
        }
        int lastDot = messageKey.lastIndexOf('.');
        String slug = lastDot >= 0 ? messageKey.substring(lastDot + 1) : messageKey;
        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "messageKey must contain a slug segment: " + messageKey);
        }
        return CUSTOM_PREFIX + slug;
    }

    /**
     * Returns {@code custom:slug} or {@code custom:slug-2}, {@code custom:slug-3}, … when the base
     * key is already taken within the same standard.
     */
    public static String allocateCustomConfigKey(String title, Predicate<String> keyExists) {
        String base = customFromTitle(title);
        if (!keyExists.test(base)) {
            return base;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = base + "-" + suffix;
            suffix++;
        } while (keyExists.test(candidate));
        return candidate;
    }

    private static String transliterateGerman(String value) {
        return value.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
    }
}
