/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.dto;

import com.emberone.platform.navigation.domain.AppLinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Input for system-managed document links (not exposed on self-service user APIs). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentLinkInput {
    private AppLinkType linkType;
    private Long linkId;
}
