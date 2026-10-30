/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.list;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.domain.ManagementActivationStatus;
import com.emberone.platform.web.list.ListSpecs;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ManagementActivationListSpecs {

    private ManagementActivationListSpecs() {}

    public static Specification<ManagementActivation> forTarget(long targetOrganizationId) {
        return (root, query, cb) ->
                cb.equal(root.get("targetOrganizationId"), targetOrganizationId);
    }

    /**
     * Words must each match person, organization names or reason. Left joins keep rows of deleted
     * users (user is null) searchable by the other fields.
     */
    public static Specification<ManagementActivation> search(String search) {
        List<String> tokens = ListSpecs.searchTokens(search);
        if (tokens.isEmpty()) {
            return ListSpecs.unconstrained();
        }
        return (root, query, cb) -> {
            Join<?, ?> user = root.join("user", JoinType.LEFT);
            Join<?, ?> managing = root.join("managingOrganization", JoinType.LEFT);
            Join<?, ?> target = root.join("targetOrganization", JoinType.LEFT);
            List<Expression<String>> fields =
                    List.of(
                            user.get("firstName"),
                            user.get("lastName"),
                            managing.get("name"),
                            target.get("name"),
                            root.get("reason"));
            List<Predicate> perToken = new ArrayList<>();
            for (String token : tokens) {
                String pattern = "%" + ListSpecs.escapeLike(token) + "%";
                perToken.add(
                        cb.or(
                                fields.stream()
                                        .map(field -> cb.like(cb.lower(field), pattern, '\\'))
                                        .toArray(Predicate[]::new)));
            }
            return cb.and(perToken.toArray(Predicate[]::new));
        };
    }

    public static Specification<ManagementActivation> matching(
            ManagementActivationListFilter filter, Instant now) {
        Specification<ManagementActivation> spec = ListSpecs.unconstrained();
        if (!filter.tenantIds().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("tenantId").in(filter.tenantIds()));
        }
        if (!filter.statuses().isEmpty()) {
            spec = spec.and(withStatuses(filter.statuses(), now));
        }
        if (!filter.organizationIds().isEmpty()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.or(
                                            root.get("targetOrganizationId")
                                                    .in(filter.organizationIds()),
                                            root.get("managingOrganizationId")
                                                    .in(filter.organizationIds())));
        }
        if (!filter.managingOrganizationIds().isEmpty()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    root.get("managingOrganizationId")
                                            .in(filter.managingOrganizationIds()));
        }
        return spec;
    }

    private static Specification<ManagementActivation> withStatuses(
            Collection<ManagementActivationStatus> statuses, Instant now) {
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            for (ManagementActivationStatus status : statuses) {
                any.add(
                        switch (status) {
                            case ACTIVE ->
                                    cb.and(
                                            cb.isNull(root.get("endedAt")),
                                            cb.greaterThan(root.<Instant>get("validUntil"), now));
                            case EXPIRED ->
                                    cb.and(
                                            cb.isNull(root.get("endedAt")),
                                            cb.lessThanOrEqualTo(
                                                    root.<Instant>get("validUntil"), now));
                            case ENDED -> cb.isNotNull(root.get("endedAt"));
                        });
            }
            return cb.or(any.toArray(Predicate[]::new));
        };
    }
}
