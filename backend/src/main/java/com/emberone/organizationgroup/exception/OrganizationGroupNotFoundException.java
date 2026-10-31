/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organizationgroup.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class OrganizationGroupNotFoundException extends ResourceNotFoundException {
    public OrganizationGroupNotFoundException(Long id) {
        super("Organization group not found with id: " + id);
    }
}
