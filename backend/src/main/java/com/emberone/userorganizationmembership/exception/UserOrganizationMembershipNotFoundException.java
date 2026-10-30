/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserOrganizationMembershipNotFoundException extends ResourceNotFoundException {
    public UserOrganizationMembershipNotFoundException(Long membershipId) {
        super("User organization membership not found with id: " + membershipId);
    }
}
