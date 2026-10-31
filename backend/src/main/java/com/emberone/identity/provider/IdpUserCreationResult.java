/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.identity.provider;

public record IdpUserCreationResult(String idpUserId, String temporaryPassword) {}
