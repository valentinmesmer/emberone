/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Input for syncing the external IdP when EmberOne user profile data changes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIdentityUserRequest {

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String privateEmail;
    private Long tenantId;
}
