/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.exception;

import com.emberone.platform.exception.ConflictException;

public class OrganizationCodeAlreadyExistsException extends ConflictException {
    public OrganizationCodeAlreadyExistsException(String code) {
        super("Organization code already exists: " + code);
    }
}
