/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.audience;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrganizationAudienceResolverTest {

    private static final OrganizationTypeDefinition DISTRICT = type(1L);
    private static final OrganizationTypeDefinition STATION = type(5L);

    // root 10 -> mid 11 -> leaf 12; other 20 stands alone
    private final Organization root = org(10L, DISTRICT, null);
    private final Organization mid = org(11L, DISTRICT, root);
    private final Organization leaf = org(12L, STATION, mid);
    private final Organization other = org(20L, STATION, null);
    private final List<Organization> all = List.of(root, mid, leaf, other);

    @Test
    void orgResolvesToTheReferencedOrganization() {
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG, 12L, all))
                .containsExactly(12L);
    }

    @Test
    void orgResolvesEvenWhenTheOrganizationIsNotListed() {
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG, 99L, all))
                .containsExactly(99L);
    }

    @Test
    void subtreeIncludesTheRootAndItsDescendants() {
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE, 11L, all))
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE, 10L, all))
                .containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void subtreeOfALeafIsOnlyTheLeaf() {
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE, 12L, all))
                .containsExactly(12L);
    }

    @Test
    void subtreeOfAnUnlistedOrganizationIsEmpty() {
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE, 99L, all))
                .isEmpty();
    }

    @Test
    void typeResolvesToAllListedOrganizationsOfThatType() {
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_TYPE, 5L, all))
                .containsExactlyInAnyOrder(12L, 20L);
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_TYPE, 6L, all))
                .isEmpty();
    }

    @Test
    void subtreeFollowsParentLinksPastAMissingIntermediateOrganization() {
        // mid (11) is not in the list, leaf (12) still belongs to root (10)'s subtree
        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE,
                                10L,
                                List.of(root, leaf)))
                .containsExactlyInAnyOrder(10L, 12L);
    }

    @Test
    void cyclicParentLinksTerminate() {
        Organization a = org(30L, DISTRICT, null);
        Organization b = org(31L, DISTRICT, a);
        a.setParentOrganization(b);

        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE, 30L, List.of(a, b)))
                .containsExactlyInAnyOrder(30L, 31L);
    }

    @Test
    void deactivationIsNotDecidedHere() {
        // The resolver is activity-agnostic: a deactivated organization stays in the tree and the
        // caller filters it out.
        Organization deactivated = org(40L, STATION, root);
        deactivated.setDeactivatedAt(Instant.now());

        assertThat(
                        OrganizationAudienceResolver.resolve(
                                OrganizationAudienceResolver.Kind.ORG_SUBTREE,
                                10L,
                                List.of(root, deactivated)))
                .containsExactlyInAnyOrder(10L, 40L);
    }

    private static OrganizationTypeDefinition type(long id) {
        OrganizationTypeDefinition type = new OrganizationTypeDefinition();
        type.setId(id);
        return type;
    }

    private static Organization org(long id, OrganizationTypeDefinition type, Organization parent) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setType(type);
        organization.setParentOrganization(parent);
        return organization;
    }
}
