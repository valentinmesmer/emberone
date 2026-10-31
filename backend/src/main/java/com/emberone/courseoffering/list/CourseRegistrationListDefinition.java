/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.platform.web.list.ListDefinition;

/**
 * Registrations of one offering; searchable by member name, {@code approvedAt,asc} is the waitlist
 * queue.
 */
public final class CourseRegistrationListDefinition {

    public static final ListDefinition<CourseRegistration> INSTANCE =
            ListDefinition.<CourseRegistration>forEntity(CourseRegistration.class)
                    .searchField("user.firstName")
                    .searchField("user.lastName")
                    .textSortKey("name", "user.lastName", "user.firstName")
                    .sortKey("registeredAt", "registeredAt")
                    .sortKey("approvedAt", "approvedAt")
                    .defaultSort("registeredAt,asc")
                    .build();

    private CourseRegistrationListDefinition() {}
}
