/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.navigation.dto;

import com.emberone.platform.navigation.domain.AppLinkType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API fields for a resolved app link. Extended by feature DTOs (documents, notifications, to-dos).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppLinkDto {
    private AppLinkType linkType;
    private Long linkId;

    /** Relative in-app route, resolved at read time. */
    private String navigationPath;
}
