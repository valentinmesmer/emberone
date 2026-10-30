/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.policy;

/**
 * Federal-state reaction when an organization-group membership ends (e.g. tie rank to office in
 * some states). Bavaria keeps the current rank.
 */
public interface RankPolicy {

    RankPolicy NO_OP = (tenantId, userId, organizationGroupId) -> {};

    void onGroupMembershipEnded(long tenantId, long userId, long organizationGroupId);
}
