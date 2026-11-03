/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.exception;

public class OrganizationMembershipAlreadyEndedException extends IllegalStateException {

    public OrganizationMembershipAlreadyEndedException(Long membershipId) {
        super("Membership is already ended: " + membershipId);
    }
}
