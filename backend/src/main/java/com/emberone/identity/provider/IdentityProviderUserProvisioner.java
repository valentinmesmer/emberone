/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.identity.provider;

import com.emberone.identity.domain.IdentityUser;

/** Provisions and syncs EmberOne users with an external identity provider (IdP). */
public interface IdentityProviderUserProvisioner {

    IdentityProviderKind kind();

    IdpUserCreationResult createUser(IdentityUser identityUser, Long tenantId, String role);

    void updateUser(IdentityUser identityUser, Long tenantId, String role);

    /** Deletes the IdP account; an already missing account counts as deleted. */
    void deleteUser(String idpUserId);
}
