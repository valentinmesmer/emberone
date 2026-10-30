/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.list;

import com.emberone.platform.web.list.ListSpecs;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.domain.UserDocumentTag;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserDocumentListSpecs {

    private UserDocumentListSpecs() {}

    public static Specification<UserDocument> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<UserDocument> tagFilter(String tag) {
        if (!StringUtils.hasText(tag)) {
            return ListSpecs.unconstrained();
        }
        String normalized = tag.trim();
        return (root, query, cb) -> {
            Subquery<Long> match = query.subquery(Long.class);
            Root<UserDocumentTag> tagRoot = match.from(UserDocumentTag.class);
            match.select(cb.literal(1L));
            match.where(
                    cb.equal(tagRoot.get("document"), root),
                    cb.equal(cb.lower(tagRoot.get("tag")), normalized.toLowerCase()));
            return cb.exists(match);
        };
    }

    public static Specification<UserDocument> viewedFilter(UserDocumentListViewed viewed) {
        return switch (viewed) {
            case ALL -> ListSpecs.unconstrained();
            case UNREAD -> (root, query, cb) -> cb.isNull(root.get("viewedAt"));
            case READ -> (root, query, cb) -> cb.isNotNull(root.get("viewedAt"));
        };
    }

    public static Specification<UserDocument> contentTypeFilter(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return ListSpecs.unconstrained();
        }
        String normalized = contentType.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("contentType")), normalized);
    }
}
