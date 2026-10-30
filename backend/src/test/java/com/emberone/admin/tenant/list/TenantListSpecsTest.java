/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.federalstate.domain.FederalState;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class TenantListSpecsTest {

    @SuppressWarnings("unchecked")
    @Test
    void matchingWithFederalStatesFiltersByInClause() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Tenant> root = mock(Root.class);
        Path<Object> federalState = mock(Path.class);
        Predicate conjunction = mock(Predicate.class);
        when(root.get("federalState")).thenReturn(federalState);
        when(cb.conjunction()).thenReturn(conjunction);
        when(federalState.in(List.of(FederalState.BAYERN, FederalState.BERLIN)))
                .thenReturn(conjunction);

        Specification<Tenant> spec =
                TenantListSpecs.matching(
                        new TenantListFilter(List.of(FederalState.BAYERN, FederalState.BERLIN)));
        spec.toPredicate(root, query, cb);

        verify(federalState).in(List.of(FederalState.BAYERN, FederalState.BERLIN));
    }

    @SuppressWarnings("unchecked")
    @Test
    void matchingWithoutFiltersIsConjunction() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Tenant> root = mock(Root.class);
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Tenant> spec = TenantListSpecs.matching(TenantListFilter.NONE);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(conjunction);
        verify(cb).conjunction();
        verify(root, org.mockito.Mockito.never()).get("federalState");
    }
}
