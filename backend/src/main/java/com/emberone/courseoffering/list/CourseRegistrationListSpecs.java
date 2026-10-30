/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

public final class CourseRegistrationListSpecs {

    private CourseRegistrationListSpecs() {}

    public static Specification<CourseRegistration> forOffering(Long offeringId) {
        return (root, query, cb) -> cb.equal(root.get("offering").get("id"), offeringId);
    }

    /** Registrations of the offerings that belong to the organization. */
    public static Specification<CourseRegistration> forOrganization(Long organizationId) {
        return (root, query, cb) ->
                cb.equal(root.get("offering").get("organizationId"), organizationId);
    }

    public static Specification<CourseRegistration> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    /** Offerings that have not ended yet, i.e. upcoming or running. */
    public static Specification<CourseRegistration> notEndedAt(Instant now) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("offering").get("lastSessionEnd"), now);
    }

    /** No statuses selected means all of them. */
    public static Specification<CourseRegistration> withStatus(
            Collection<CourseRegistrationStatus> statuses) {
        return (root, query, cb) ->
                statuses.isEmpty() ? cb.conjunction() : root.get("status").in(statuses);
    }
}
