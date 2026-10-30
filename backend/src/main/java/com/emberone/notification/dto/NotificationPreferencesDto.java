/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record NotificationPreferencesDto(List<NotificationPreferenceDto> preferences) {}
