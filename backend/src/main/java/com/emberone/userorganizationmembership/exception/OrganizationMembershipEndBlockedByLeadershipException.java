/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.exception;

import com.emberone.platform.exception.ValidationException;

public class OrganizationMembershipEndBlockedByLeadershipException extends ValidationException {

    public OrganizationMembershipEndBlockedByLeadershipException() {
        super("Cannot change organization membership while the user is in the leadership group");
    }
}
