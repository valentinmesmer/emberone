/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CourseAudienceMatcherTest {

    private static final long PROVIDER_ORG = 10L;

    // user: member of org 12 (root 10 -> 11 -> 12) and of the org type 5
    private static final CourseAudienceScope SCOPE =
            new CourseAudienceScope(
                    Set.of(12L), Set.of(10L, 11L, 12L), Set.of(5L), Map.of(10L, 0, 11L, 1, 12L, 2));

    @Test
    void matchesFollowTheSameRulesAsTheEligibilityQuery() {
        CourseOffering offering = offering();
        assertThat(matches(offering, CourseAudienceKind.ORG, 12L)).isTrue();
        assertThat(matches(offering, CourseAudienceKind.ORG, 11L)).isFalse();
        assertThat(matches(offering, CourseAudienceKind.ORG_TYPE, 5L)).isTrue();
        assertThat(matches(offering, CourseAudienceKind.ORG_TYPE, 6L)).isFalse();
        // a subtree entry matches via any ancestor of a membership
        assertThat(matches(offering, CourseAudienceKind.ORG_SUBTREE, 11L)).isTrue();
        assertThat(matches(offering, CourseAudienceKind.ORG_SUBTREE, 12L)).isTrue();
        assertThat(matches(offering, CourseAudienceKind.ORG_SUBTREE, 99L)).isFalse();
        // own org: member of the offering's organization (not the case here)
        assertThat(matches(offering, CourseAudienceKind.OWN_ORG, null)).isFalse();
    }

    @Test
    void ownOrgMatchesMembersOfTheProvider() {
        CourseAudienceScope member =
                new CourseAudienceScope(Set.of(PROVIDER_ORG), Set.of(PROVIDER_ORG), Set.of());
        assertThat(
                        CourseAudienceMatcher.matches(
                                entry(1L, CourseAudienceKind.OWN_ORG, null, null),
                                PROVIDER_ORG,
                                member))
                .isTrue();
    }

    @Test
    void withoutMembershipNothingMatches() {
        CourseAudienceScope none = new CourseAudienceScope(Set.of(), Set.of(), Set.of());
        assertThat(
                        CourseAudienceMatcher.matches(
                                entry(1L, CourseAudienceKind.ORG, 12L, null), PROVIDER_ORG, none))
                .isFalse();
    }

    @Test
    void sortsSingleOrgBeforeDeeperSubtreeBeforeHigherSubtreeBeforeType() {
        CourseOffering offering = offering();
        offering.getAudiences().add(entry(1L, CourseAudienceKind.ORG_TYPE, 5L, 1));
        offering.getAudiences().add(entry(2L, CourseAudienceKind.ORG_SUBTREE, 10L, 1));
        offering.getAudiences().add(entry(3L, CourseAudienceKind.ORG_SUBTREE, 12L, 1));
        offering.getAudiences().add(entry(4L, CourseAudienceKind.ORG, 12L, 1));
        offering.getAudiences().add(entry(5L, CourseAudienceKind.ORG_SUBTREE, 11L, 1));
        offering.getAudiences().add(entry(6L, CourseAudienceKind.ORG, 77L, 1)); // no match

        assertThat(CourseAudienceMatcher.matching(offering, SCOPE))
                .extracting(CourseOfferingAudience::getId)
                .containsExactly(4L, 3L, 5L, 2L, 1L);
    }

    @Test
    void equallySpecificEntriesAreOrderedById() {
        CourseAudienceScope twoOrgs =
                new CourseAudienceScope(Set.of(12L, 13L), Set.of(12L, 13L), Set.of());
        CourseOffering offering = offering();
        offering.getAudiences().add(entry(9L, CourseAudienceKind.ORG, 13L, 1));
        offering.getAudiences().add(entry(8L, CourseAudienceKind.ORG, 12L, 1));

        assertThat(CourseAudienceMatcher.matching(offering, twoOrgs))
                .extracting(CourseOfferingAudience::getId)
                .containsExactly(8L, 9L);
    }

    @Test
    void firstWithFreeSeatSkipsUsedUpQuotas() {
        CourseOfferingAudience first = entry(1L, CourseAudienceKind.ORG, 12L, 2);
        CourseOfferingAudience second = entry(2L, CourseAudienceKind.ORG_TYPE, 5L, 2);

        assertThat(CourseAudienceMatcher.firstWithFreeSeat(List.of(first, second), Map.of(1L, 1L)))
                .contains(first);
        assertThat(CourseAudienceMatcher.firstWithFreeSeat(List.of(first, second), Map.of(1L, 2L)))
                .contains(second);
        assertThat(
                        CourseAudienceMatcher.firstWithFreeSeat(
                                List.of(first, second), Map.of(1L, 2L, 2L, 2L)))
                .isEmpty();
    }

    @Test
    void publicOfferingWithNoAudienceRowsCoversAllOrganizations() {
        com.emberone.organization.domain.OrganizationTypeDefinition station = type(5L);
        com.emberone.organization.domain.Organization a = org(10L, station, null);
        com.emberone.organization.domain.Organization b = org(20L, station, null);
        List<com.emberone.organization.domain.Organization> all = List.of(a, b);
        CourseOffering offering = offering();

        assertThat(CourseAudienceMatcher.eligibleOrganizationIds(offering, all))
                .containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void eligibleOrganizationsAreTheInverseOfTheScopeMatch() {
        com.emberone.organization.domain.OrganizationTypeDefinition district = type(1L);
        com.emberone.organization.domain.OrganizationTypeDefinition station = type(5L);
        com.emberone.organization.domain.Organization root = org(10L, district, null);
        com.emberone.organization.domain.Organization mid = org(11L, district, root);
        com.emberone.organization.domain.Organization leaf = org(12L, station, mid);
        com.emberone.organization.domain.Organization other = org(20L, station, null);
        List<com.emberone.organization.domain.Organization> all = List.of(root, mid, leaf, other);

        assertThat(eligible(CourseAudienceKind.OWN_ORG, null, all)).containsExactly(PROVIDER_ORG);
        assertThat(eligible(CourseAudienceKind.ORG, 12L, all)).containsExactly(12L);
        assertThat(eligible(CourseAudienceKind.ORG_TYPE, 5L, all))
                .containsExactlyInAnyOrder(12L, 20L);
        assertThat(eligible(CourseAudienceKind.ORG_SUBTREE, 11L, all))
                .containsExactlyInAnyOrder(11L, 12L);
    }

    private static Set<Long> eligible(
            CourseAudienceKind kind,
            Long refId,
            List<com.emberone.organization.domain.Organization> organizations) {
        CourseOffering offering = offering();
        offering.getAudiences().add(entry(1L, kind, refId, null));
        return CourseAudienceMatcher.eligibleOrganizationIds(offering, organizations);
    }

    private static com.emberone.organization.domain.OrganizationTypeDefinition type(long id) {
        com.emberone.organization.domain.OrganizationTypeDefinition type =
                new com.emberone.organization.domain.OrganizationTypeDefinition();
        type.setId(id);
        return type;
    }

    private static com.emberone.organization.domain.Organization org(
            long id,
            com.emberone.organization.domain.OrganizationTypeDefinition type,
            com.emberone.organization.domain.Organization parent) {
        com.emberone.organization.domain.Organization organization =
                new com.emberone.organization.domain.Organization();
        organization.setId(id);
        organization.setType(type);
        organization.setParentOrganization(parent);
        return organization;
    }

    private static boolean matches(CourseOffering offering, CourseAudienceKind kind, Long refId) {
        return CourseAudienceMatcher.matches(
                entry(1L, kind, refId, null), offering.getOrganizationId(), SCOPE);
    }

    private static CourseOffering offering() {
        return CourseOffering.builder().id(100L).tenantId(7L).organizationId(PROVIDER_ORG).build();
    }

    private static CourseOfferingAudience entry(
            Long id, CourseAudienceKind kind, Long refId, Integer quota) {
        return CourseOfferingAudience.builder()
                .id(id)
                .tenantId(7L)
                .kind(kind)
                .refId(refId)
                .quota(quota)
                .build();
    }
}
