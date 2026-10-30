/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.repository;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    @Query(
            """
        SELECT COUNT(n) FROM Notification n
        WHERE n.recipientUserId = :recipientUserId
        AND n.readAt IS NULL
        AND EXISTS (
            SELECT 1 FROM NotificationDelivery d
            WHERE d.notification = n AND d.channel = :inAppChannel
        )
        """)
    long countUnreadInAppForRecipient(
            @Param("recipientUserId") Long recipientUserId,
            @Param("inAppChannel") NotificationChannel inAppChannel);

    @Query(
            """
        SELECT DISTINCT n.type FROM Notification n
        WHERE n.recipientUserId = :recipientUserId
        AND EXISTS (
            SELECT 1 FROM NotificationDelivery d
            WHERE d.notification = n AND d.channel = :inAppChannel
        )
        """)
    List<NotificationType> findDistinctInAppTypesForRecipient(
            @Param("recipientUserId") Long recipientUserId,
            @Param("inAppChannel") NotificationChannel inAppChannel);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    @Transactional
    @Modifying
    @Query(
            """
        UPDATE Notification n SET n.readAt = :readAt
        WHERE n.recipientUserId = :recipientUserId
        AND n.readAt IS NULL
        AND EXISTS (
            SELECT 1 FROM NotificationDelivery d
            WHERE d.notification = n AND d.channel = :inAppChannel
        )
        """)
    int markAllReadForRecipient(
            @Param("recipientUserId") Long recipientUserId,
            @Param("readAt") Instant readAt,
            @Param("inAppChannel") NotificationChannel inAppChannel);
}
