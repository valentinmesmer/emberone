/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.list;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.domain.NotificationType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationListSpecs {

    private NotificationListSpecs() {}

    public static Specification<Notification> forRecipient(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("recipientUserId"), userId);
    }

    public static Specification<Notification> inAppChannel() {
        return (root, query, cb) -> {
            Subquery<Long> delivery = query.subquery(Long.class);
            Root<NotificationDelivery> deliveryRoot = delivery.from(NotificationDelivery.class);
            delivery.select(cb.literal(1L));
            delivery.where(
                    cb.equal(deliveryRoot.get("notification"), root),
                    cb.equal(deliveryRoot.get("channel"), NotificationChannel.IN_APP));
            return cb.exists(delivery);
        };
    }

    public static Specification<Notification> statusFilter(NotificationListStatus status) {
        return switch (status) {
            case UNREAD -> (root, query, cb) -> cb.isNull(root.get("readAt"));
            case READ -> (root, query, cb) -> cb.isNotNull(root.get("readAt"));
        };
    }

    public static Specification<Notification> statusesFilter(
            List<NotificationListStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Specification<Notification>> buckets = new ArrayList<>();
        for (NotificationListStatus status : statuses) {
            buckets.add(statusFilter(status));
        }
        return Specification.anyOf(buckets);
    }

    public static Specification<Notification> typesFilter(List<NotificationType> types) {
        if (types == null || types.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("type").in(types);
    }
}
