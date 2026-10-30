/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class UserOrganizationGroupMembershipNotFoundException extends ResourceNotFoundException {
    public UserOrganizationGroupMembershipNotFoundException(Long membershipId) {
        super("User organization group membership not found with id: " + membershipId);
    }
}
