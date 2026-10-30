/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.exception;

public class OrganizationMembershipAlreadyEndedException extends IllegalStateException {

    public OrganizationMembershipAlreadyEndedException(Long membershipId) {
        super("Membership is already ended: " + membershipId);
    }
}
