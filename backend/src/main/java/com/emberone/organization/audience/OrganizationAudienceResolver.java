/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.audience;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.service.OrganizationSubtreeSupport;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves an organization audience entry to the organizations it covers within one tenant. Shared
 * by every feature that scopes something to organizations, so the meaning of {@code ORG}, {@code
 * ORG_SUBTREE} and {@code ORG_TYPE} is defined once.
 *
 * <p>The resolver is pure and does not look at activity: the caller passes the organizations it
 * considers and decides how to treat deactivated ones.
 */
public final class OrganizationAudienceResolver {

    /** The kinds of audience entry that point at organizations. */
    public enum Kind {
        ORG,
        ORG_SUBTREE,
        ORG_TYPE
    }

    private OrganizationAudienceResolver() {}

    /**
     * The ids the entry covers.
     *
     * <ul>
     *   <li>{@code ORG}: the referenced organization itself, also when it is not in the list.
     *   <li>{@code ORG_SUBTREE}: every listed organization whose parent chain reaches the
     *       referenced organization, itself included. The chain is followed through the parent
     *       links, so a missing intermediate entry in the list does not cut it. Cyclic links cannot
     *       make this loop.
     *   <li>{@code ORG_TYPE}: every listed organization of that type.
     * </ul>
     *
     * @param organizationsInTenant all organizations of the tenant the caller considers
     */
    public static Set<Long> resolve(
            Kind kind, long refId, List<Organization> organizationsInTenant) {
        return switch (kind) {
            case ORG -> Set.of(refId);
            case ORG_SUBTREE ->
                    organizationsInTenant.stream()
                            .filter(
                                    organization ->
                                            OrganizationSubtreeSupport.isSelfOrDescendant(
                                                    organization, refId))
                            .map(Organization::getId)
                            .collect(Collectors.toSet());
            case ORG_TYPE ->
                    organizationsInTenant.stream()
                            .filter(organization -> organization.getType().getId() == refId)
                            .map(Organization::getId)
                            .collect(Collectors.toSet());
        };
    }
}
