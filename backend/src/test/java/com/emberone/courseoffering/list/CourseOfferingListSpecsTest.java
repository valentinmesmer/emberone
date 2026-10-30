/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.service.CourseAudienceScope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CourseOfferingListSpecsTest {

    @SuppressWarnings("unchecked")
    private final Root<CourseOffering> root = mock(Root.class);

    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @Test
    void userWithoutActiveMembershipIsEligibleForNothingNotEvenTenantWideOfferings() {
        Predicate nothing = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(nothing);
        CourseAudienceScope noMembership = new CourseAudienceScope(Set.of(), Set.of(), Set.of());

        Predicate predicate =
                CourseOfferingListSpecs.eligibleFor(noMembership).toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(nothing);
        verify(cb).disjunction();
        verifyNoInteractions(query);
    }
}
