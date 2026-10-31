/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permissiongroup.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class PermissionGroupNotFoundException extends ResourceNotFoundException {
    public PermissionGroupNotFoundException(Long id) {
        super("Permission group not found with id: " + id);
    }
}
