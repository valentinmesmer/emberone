/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.mapper;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.dto.NotificationDto;
import com.emberone.platform.navigation.AppNavigationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AppNavigationResolver.class)
public interface NotificationMapper {

    @Mapping(target = "navigationPath", source = "entity", qualifiedByName = "navigationPath")
    @Mapping(target = "unread", expression = "java(entity.getReadAt() == null)")
    @Mapping(
            target = "type",
            expression = "java(entity.getType() != null ? entity.getType().name() : null)")
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "body", ignore = true)
    NotificationDto toDto(Notification entity);
}
