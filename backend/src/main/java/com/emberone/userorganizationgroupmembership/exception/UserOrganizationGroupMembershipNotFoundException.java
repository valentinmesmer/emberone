/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationgroupmembership.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserOrganizationGroupMembershipNotFoundException extends ResourceNotFoundException {
    public UserOrganizationGroupMembershipNotFoundException(Long membershipId) {
        super("User organization group membership not found with id: " + membershipId);
    }
}
