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

/** Input for provisioning an external IdP identity when an EmberOne user is created. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionIdentityUserRequest {

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String privateEmail;

    /** Application role name for identity (e.g. first user vs. regular user). */
    private String role;

    private Long tenantId;
}
