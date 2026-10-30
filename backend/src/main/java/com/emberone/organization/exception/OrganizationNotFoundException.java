/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class OrganizationNotFoundException extends ResourceNotFoundException {
    public OrganizationNotFoundException(Long id) {
        super("Organization not found with id: " + id);
    }
}
