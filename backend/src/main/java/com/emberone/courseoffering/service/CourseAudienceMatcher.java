/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.CourseOfferingAudiencePolicy;
import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.organization.audience.OrganizationAudienceResolver;
import com.emberone.organization.domain.Organization;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory counterpart of {@code CourseOfferingListSpecs.eligibleFor}: which audience entries of
 * an offering a user's {@link CourseAudienceScope} hits, and which of them is the most specific.
 * Both use the same scope and the same matching rules, so eligibility and quota assignment cannot
 * drift apart.
 *
 * <p>Specificity, most specific first: a single organization, then a subtree (the deeper the
 * subtree's root, the more specific), then an organization type. Remaining ties are broken by entry
 * id so the choice is deterministic.
 */
final class CourseAudienceMatcher {

    private CourseAudienceMatcher() {}

    /** Whether the user's scope hits this entry of the offering. */
    static boolean matches(
            CourseOfferingAudience entry, Long offeringOrganizationId, CourseAudienceScope scope) {
        if (scope.memberOrganizationIds().isEmpty()) {
            return false;
        }
        return switch (entry.getKind()) {
            case OWN_ORG -> scope.memberOrganizationIds().contains(offeringOrganizationId);
            case ORG -> scope.memberOrganizationIds().contains(entry.getRefId());
            case ORG_TYPE -> scope.organizationTypeIds().contains(entry.getRefId());
            case ORG_SUBTREE -> scope.ancestorOrSelfOrganizationIds().contains(entry.getRefId());
        };
    }

    /**
     * Inverse of {@link #matches}: the organizations whose members the audience of the offering
     * covers. A user is eligible exactly when they have an active membership in one of them, which
     * is the same rule as a scope hitting an entry.
     */
    static Set<Long> eligibleOrganizationIds(
            CourseOffering offering, List<Organization> organizations) {
        if (CourseOfferingAudiencePolicy.isPublicUnrestricted(offering)) {
            Set<Long> ids = new HashSet<>();
            organizations.forEach(organization -> ids.add(organization.getId()));
            return ids;
        }
        Set<Long> ids = new HashSet<>();
        for (CourseOfferingAudience entry : offering.getAudiences()) {
            switch (entry.getKind()) {
                case OWN_ORG -> ids.add(offering.getOrganizationId());
                case ORG ->
                        ids.addAll(
                                OrganizationAudienceResolver.resolve(
                                        OrganizationAudienceResolver.Kind.ORG,
                                        entry.getRefId(),
                                        organizations));
                case ORG_TYPE ->
                        ids.addAll(
                                OrganizationAudienceResolver.resolve(
                                        OrganizationAudienceResolver.Kind.ORG_TYPE,
                                        entry.getRefId(),
                                        organizations));
                case ORG_SUBTREE ->
                        ids.addAll(
                                OrganizationAudienceResolver.resolve(
                                        OrganizationAudienceResolver.Kind.ORG_SUBTREE,
                                        entry.getRefId(),
                                        organizations));
            }
        }
        return ids;
    }

    /** The entries of the offering that the scope hits, most specific first. */
    static List<CourseOfferingAudience> matching(
            CourseOffering offering, CourseAudienceScope scope) {
        return offering.getAudiences().stream()
                .filter(entry -> matches(entry, offering.getOrganizationId(), scope))
                .sorted(mostSpecificFirst(scope))
                .toList();
    }

    /**
     * The first of the given entries (already most specific first) that still has a free quota
     * seat.
     *
     * @param takenByAudience participants per audience entry id
     */
    static Optional<CourseOfferingAudience> firstWithFreeSeat(
            List<CourseOfferingAudience> matching, Map<Long, Long> takenByAudience) {
        return matching.stream()
                .filter(entry -> takenByAudience.getOrDefault(entry.getId(), 0L) < entry.getQuota())
                .findFirst();
    }

    private static Comparator<CourseOfferingAudience> mostSpecificFirst(CourseAudienceScope scope) {
        return Comparator.comparingInt((CourseOfferingAudience e) -> -rank(e.getKind()))
                .thenComparingInt(e -> -subtreeDepth(e, scope))
                .thenComparing(
                        CourseOfferingAudience::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static int rank(CourseAudienceKind kind) {
        return switch (kind) {
            case ORG -> 3;
            case ORG_SUBTREE -> 2;
            case ORG_TYPE, OWN_ORG -> 1;
        };
    }

    private static int subtreeDepth(CourseOfferingAudience entry, CourseAudienceScope scope) {
        return entry.getKind() == CourseAudienceKind.ORG_SUBTREE
                ? scope.depthByOrganizationId().getOrDefault(entry.getRefId(), 0)
                : 0;
    }
}
