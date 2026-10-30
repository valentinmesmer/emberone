/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.list;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.platform.web.list.ListDefinition;

public final class OnboardingInvitationListDefinition {

    public static final ListDefinition<OnboardingInvitation> INSTANCE =
            ListDefinition.<OnboardingInvitation>forEntity(OnboardingInvitation.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .textSortKey("lastName", "user.lastName", "user.firstName")
                    .defaultSort("lastName,asc")
                    .build();

    private OnboardingInvitationListDefinition() {}
}
