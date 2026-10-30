/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.exception;

import com.emberone.platform.exception.ConflictException;

public class UserOrganizationMembershipAlreadyExistsException extends ConflictException {
    public UserOrganizationMembershipAlreadyExistsException(Long userId, Long organizationId) {
        super("User " + userId + " is already a member of organization " + organizationId);
    }
}
