/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.list;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.userdocument.domain.UserDocument;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class UserDocumentListSpecsTest {

    @SuppressWarnings("unchecked")
    private static void apply(
            Specification<UserDocument> spec, CriteriaBuilder cb, Root<UserDocument> root) {
        spec.toPredicate(root, mock(CriteriaQuery.class), cb);
    }

    @SuppressWarnings("unchecked")
    @Test
    void contentTypeFilterMatchesLowercaseMime() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<UserDocument> root = mock(Root.class);
        Path<Object> contentType = mock(Path.class);
        when(root.get("contentType")).thenReturn(contentType);
        when(cb.lower(any())).thenReturn(mock(Path.class));
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));

        apply(UserDocumentListSpecs.contentTypeFilter("application/pdf"), cb, root);

        verify(cb).equal(any(), org.mockito.ArgumentMatchers.eq("application/pdf"));
    }

    @Test
    void viewedUnreadIsNullViewedAt() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<UserDocument> root = mock(Root.class);
        Path<Object> viewedAt = mock(Path.class);
        when(root.get("viewedAt")).thenReturn(viewedAt);
        when(cb.isNull(viewedAt)).thenReturn(mock(Predicate.class));

        apply(UserDocumentListSpecs.viewedFilter(UserDocumentListViewed.UNREAD), cb, root);

        verify(cb).isNull(viewedAt);
    }
}
