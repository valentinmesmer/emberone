/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organization.exception;

import com.emberone.platform.exception.ConflictException;

public class OrganizationCodeAlreadyExistsException extends ConflictException {
    public OrganizationCodeAlreadyExistsException(String code) {
        super("Organization code already exists: " + code);
    }
}
