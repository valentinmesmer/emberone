/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.service.CourseAudienceScope;
import java.util.List;

/** Public (no rows or restrictions only) vs private ({@code OWN_ORG} alone). */
public final class CourseOfferingAudiencePolicy {

    private CourseOfferingAudiencePolicy() {}

    /** No audience rows: open to the whole tenant. */
    public static boolean isPublicUnrestricted(CourseOffering offering) {
        return offering.getAudiences().isEmpty();
    }

    /** Exactly one {@code OWN_ORG} entry and nothing else. */
    public static boolean isPrivate(CourseOffering offering) {
        List<CourseOfferingAudience> audiences = offering.getAudiences();
        return audiences.size() == 1
                && audiences.getFirst().getKind() == CourseAudienceKind.OWN_ORG
                && audiences.getFirst().getRefId() == null
                && audiences.getFirst().getQuota() == null;
    }

    /** Public with {@code ORG} / {@code ORG_TYPE} / {@code ORG_SUBTREE} filters. */
    public static boolean isPublicRestricted(CourseOffering offering) {
        return offering.getAudiences().stream().allMatch(entry -> !entry.getKind().isExclusive());
    }

    public static boolean isPublic(CourseOffering offering) {
        return isPublicUnrestricted(offering)
                || (isPublicRestricted(offering) && !isPrivate(offering));
    }

    public static boolean userMayAccessPublicOffering(CourseAudienceScope scope) {
        return !scope.memberOrganizationIds().isEmpty();
    }
}
