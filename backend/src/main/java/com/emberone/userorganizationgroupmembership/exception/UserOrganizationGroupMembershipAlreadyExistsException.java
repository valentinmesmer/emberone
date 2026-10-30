/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.exception;

import com.emberone.platform.exception.ConflictException;

public class UserOrganizationGroupMembershipAlreadyExistsException extends ConflictException {
    public UserOrganizationGroupMembershipAlreadyExistsException(
            Long userId, Long organizationGroupId) {
        super(
                "User "
                        + userId
                        + " is already a member of organization group "
                        + organizationGroupId);
    }
}
