/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.list;

import com.emberone.courseoffering.CourseOfferingAudiencePolicy;
import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.service.CourseAudienceScope;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class CourseOfferingListSpecs {

    public static final Set<CourseRegistrationStatus> ACTIVE_SELF_REGISTRATION_STATUSES =
            EnumSet.of(
                    CourseRegistrationStatus.PENDING,
                    CourseRegistrationStatus.PARTICIPANT,
                    CourseRegistrationStatus.WAITLIST);

    private CourseOfferingListSpecs() {}

    public static Specification<CourseOffering> forOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    /** Offerings only; templates are listed separately via {@link #templates()}. */
    public static Specification<CourseOffering> withStatus(
            Collection<CourseOfferingStatus> statuses) {
        return (root, query, cb) ->
                statuses.isEmpty()
                        ? cb.notEqual(root.get("status"), CourseOfferingStatus.TEMPLATE)
                        : root.get("status").in(statuses);
    }

    public static Specification<CourseOffering> templates() {
        return (root, query, cb) -> cb.equal(root.get("status"), CourseOfferingStatus.TEMPLATE);
    }

    public static Specification<CourseOffering> withId(Long offeringId) {
        return (root, query, cb) -> cb.equal(root.get("id"), offeringId);
    }

    public static Specification<CourseOffering> published() {
        return (root, query, cb) -> cb.equal(root.get("status"), CourseOfferingStatus.PUBLISHED);
    }

    /** Registration has not ended yet (still open or opening later). */
    public static Specification<CourseOffering> registrationNotEnded(Instant now) {
        return (root, query, cb) -> cb.greaterThan(root.get("registrationEnd"), now);
    }

    public static Specification<CourseOffering> withTrainingCourseTypeIds(
            Collection<Long> trainingCourseTypeIds) {
        return (root, query, cb) ->
                trainingCourseTypeIds.isEmpty()
                        ? cb.conjunction()
                        : root.get("trainingCourseTypeId").in(trainingCourseTypeIds);
    }

    public static Specification<CourseOffering> idIn(Collection<Long> offeringIds) {
        return (root, query, cb) ->
                offeringIds.isEmpty() ? cb.disjunction() : root.get("id").in(offeringIds);
    }

    public static Specification<CourseOffering> idNotIn(Collection<Long> offeringIds) {
        return (root, query, cb) ->
                offeringIds.isEmpty() ? cb.conjunction() : cb.not(root.get("id").in(offeringIds));
    }

    public static Specification<CourseOffering> typeNotIn(Collection<Long> trainingCourseTypeIds) {
        return (root, query, cb) ->
                trainingCourseTypeIds.isEmpty()
                        ? cb.conjunction()
                        : cb.not(root.get("trainingCourseTypeId").in(trainingCourseTypeIds));
    }

    /**
     * At least one audience entry of the offering matches the user's scope. Without an active
     * membership nothing matches, not even tenant-wide offerings.
     */
    /** Caller has an active registration on the offering. */
    public static Specification<CourseOffering> userHasActiveRegistration(Long userId) {
        return userHasRegistration(userId, ACTIVE_SELF_REGISTRATION_STATUSES);
    }

    /** Caller has a registration in one of the given statuses on the offering. */
    public static Specification<CourseOffering> userHasRegistration(
            Long userId, Collection<CourseRegistrationStatus> statuses) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<CourseRegistration> registration = sub.from(CourseRegistration.class);
            sub.select(registration.get("id"))
                    .where(
                            cb.equal(registration.get("userId"), userId),
                            cb.equal(registration.get("offering"), root),
                            registration.get("status").in(statuses));
            return cb.exists(sub);
        };
    }

    /** Registration window is open at {@code now}. */
    public static Specification<CourseOffering> registrationOpen(Instant now) {
        return (root, query, cb) ->
                cb.and(
                        cb.lessThanOrEqualTo(root.get("registrationStart"), now),
                        cb.greaterThan(root.get("registrationEnd"), now));
    }

    public static Specification<CourseOffering> eligibleFor(CourseAudienceScope scope) {
        return (root, query, cb) -> {
            if (!CourseOfferingAudiencePolicy.userMayAccessPublicOffering(scope)) {
                return cb.disjunction();
            }
            Subquery<Long> anyAudience = query.subquery(Long.class);
            Root<CourseOfferingAudience> anyRoot = anyAudience.from(CourseOfferingAudience.class);
            anyAudience.select(anyRoot.get("id")).where(cb.equal(anyRoot.get("offering"), root));
            Predicate publicOffering = cb.not(cb.exists(anyAudience));

            Subquery<Long> sub = query.subquery(Long.class);
            Root<CourseOfferingAudience> audience = sub.from(CourseOfferingAudience.class);
            List<Predicate> matches = new ArrayList<>();
            matches.add(
                    cb.and(
                            cb.equal(audience.get("kind"), CourseAudienceKind.OWN_ORG),
                            root.get("organizationId").in(scope.memberOrganizationIds())));
            matches.add(
                    cb.and(
                            cb.equal(audience.get("kind"), CourseAudienceKind.ORG),
                            audience.get("refId").in(scope.memberOrganizationIds())));
            if (!scope.organizationTypeIds().isEmpty()) {
                matches.add(
                        cb.and(
                                cb.equal(audience.get("kind"), CourseAudienceKind.ORG_TYPE),
                                audience.get("refId").in(scope.organizationTypeIds())));
            }
            if (!scope.ancestorOrSelfOrganizationIds().isEmpty()) {
                matches.add(
                        cb.and(
                                cb.equal(audience.get("kind"), CourseAudienceKind.ORG_SUBTREE),
                                audience.get("refId").in(scope.ancestorOrSelfOrganizationIds())));
            }
            sub.select(audience.get("id"))
                    .where(
                            cb.equal(audience.get("offering"), root),
                            cb.or(matches.toArray(new Predicate[0])));
            return cb.or(publicOffering, cb.exists(sub));
        };
    }
}
