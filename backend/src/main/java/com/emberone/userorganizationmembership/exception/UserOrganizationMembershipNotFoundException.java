/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserOrganizationMembershipNotFoundException extends ResourceNotFoundException {
    public UserOrganizationMembershipNotFoundException(Long membershipId) {
        super("User organization membership not found with id: " + membershipId);
    }
}
