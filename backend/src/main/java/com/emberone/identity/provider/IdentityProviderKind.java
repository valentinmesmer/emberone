/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.provider;

/**
 * Supported external identity providers for user provisioning and OIDC login. Add a new enum
 * constant and a {@link IdentityProviderUserProvisioner} implementation to integrate another IdP.
 */
public enum IdentityProviderKind {
    AUTHENTIK
}
