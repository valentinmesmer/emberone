/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.notification.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record NotificationPreferencesDto(List<NotificationPreferenceDto> preferences) {}
