/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class OrganizationGroupNotFoundException extends ResourceNotFoundException {
    public OrganizationGroupNotFoundException(Long id) {
        super("Organization group not found with id: " + id);
    }
}
