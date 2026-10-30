/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.channel.email.EmailRenderContext;
import com.emberone.notification.channel.email.EmailRenderResult;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bundles all type-specific notification behaviour: param enrichment for i18n templates, e-mail
 * rendering, and default delivery channels before user overrides. Register one Spring bean per
 * {@link NotificationType}; the {@link NotificationTypeDefinitionRegistry} validates that every
 * enum value has a definition.
 */
public interface NotificationTypeDefinition {

    NotificationType type();

    /** Channels enabled by default until the user overrides them. */
    Set<NotificationChannel> defaultChannels();

    /**
     * Raw param keys that must not be copied verbatim into display params because {@link
     * #enrichDisplayParams} transforms them.
     */
    default Collection<String> derivedSourceKeys() {
        return List.of();
    }

    /** Adds derived display values for i18n interpolation (in-app and default e-mail body). */
    default void enrichDisplayParams(
            Map<String, Object> rawParams, Map<String, Object> display, SupportedLocale locale) {}

    /**
     * Prefix for i18n keys ({@code prefix + ".title"} / {@code prefix + ".body"}). Defaults to
     * {@code notification.<type>}; override when templates live elsewhere.
     */
    default String messageKeyPrefix(Notification notification) {
        return "notification." + type().name();
    }

    EmailRenderResult renderEmail(EmailRenderContext context);
}
