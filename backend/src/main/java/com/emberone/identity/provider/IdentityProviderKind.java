/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.identity.provider;

/**
 * Supported external identity providers for user provisioning and OIDC login. Add a new enum
 * constant and a {@link IdentityProviderUserProvisioner} implementation to integrate another IdP.
 */
public enum IdentityProviderKind {
    AUTHENTIK
}
