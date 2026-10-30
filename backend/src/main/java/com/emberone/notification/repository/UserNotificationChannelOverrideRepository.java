/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.repository;

import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.domain.UserNotificationChannelOverride;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserNotificationChannelOverrideRepository
        extends JpaRepository<UserNotificationChannelOverride, Long> {

    List<UserNotificationChannelOverride> findAllByUserId(Long userId);

    List<UserNotificationChannelOverride> findAllByUserIdAndNotificationType(
            Long userId, NotificationType notificationType);

    Optional<UserNotificationChannelOverride> findByUserIdAndNotificationTypeAndChannel(
            Long userId, NotificationType notificationType, NotificationChannel channel);

    @Transactional
    void deleteByUserIdAndNotificationTypeAndChannel(
            Long userId, NotificationType notificationType, NotificationChannel channel);
}
