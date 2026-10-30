/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.dto;

import com.emberone.platform.navigation.dto.AppLinkDto;
import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationDto extends AppLinkDto {
    private Long id;
    private String type;
    private String title;
    private String body;
    private Map<String, Object> params;
    private Instant readAt;
    private boolean unread;
}
