/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAudienceScopeServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 3L;

    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock OrganizationRepository organizationRepository;

    @InjectMocks CourseAudienceScopeService service;

    @Test
    void scopeCoversMembershipsAncestorsAndTypes() {
        OrganizationTypeDefinition district = type(1L);
        OrganizationTypeDefinition station = type(2L);
        Organization root = org(1L, district, null);
        Organization mid = org(2L, district, root);
        Organization leaf = org(3L, station, mid);
        Organization sibling = org(4L, station, root);
        when(organizationRepository.findAllWithParentFetch())
                .thenReturn(List.of(root, mid, leaf, sibling));
        when(membershipRepository.findActiveByUser_Id(any(), any()))
                .thenReturn(List.of(membership(leaf)));

        CourseAudienceScope scope = service.forUser(USER_ID);

        assertThat(scope.memberOrganizationIds()).containsExactly(3L);
        assertThat(scope.ancestorOrSelfOrganizationIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(scope.organizationTypeIds()).containsExactly(2L);
        assertThat(scope.depthByOrganizationId())
                .containsEntry(1L, 0)
                .containsEntry(2L, 1)
                .containsEntry(3L, 2)
                .doesNotContainKey(4L);
    }

    @Test
    void depthIsIndependentOfTheOrderOfMemberships() {
        OrganizationTypeDefinition station = type(2L);
        Organization root = org(1L, station, null);
        Organization mid = org(2L, station, root);
        Organization leaf = org(3L, station, mid);
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(root, mid, leaf));
        // the mid membership is walked first, so the leaf's walk stops early at mid
        when(membershipRepository.findActiveByUser_Id(any(), any()))
                .thenReturn(List.of(membership(mid), membership(leaf)));

        CourseAudienceScope scope = service.forUser(USER_ID);

        assertThat(scope.depthByOrganizationId())
                .containsEntry(1L, 0)
                .containsEntry(2L, 1)
                .containsEntry(3L, 2);
    }

    @Test
    void userWithoutMembershipHasEmptyScope() {
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of());
        when(membershipRepository.findActiveByUser_Id(any(), any())).thenReturn(List.of());

        CourseAudienceScope scope = service.forUser(USER_ID);

        assertThat(scope.memberOrganizationIds()).isEmpty();
        assertThat(scope.ancestorOrSelfOrganizationIds()).isEmpty();
        assertThat(scope.organizationTypeIds()).isEmpty();
    }

    @Test
    void multipleMembershipsAreUnioned() {
        OrganizationTypeDefinition station = type(2L);
        Organization root = org(1L, station, null);
        Organization a = org(2L, station, root);
        Organization b = org(3L, station, root);
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(root, a, b));
        when(membershipRepository.findActiveByUser_Id(any(), any()))
                .thenReturn(List.of(membership(a), membership(b)));

        CourseAudienceScope scope = service.forUser(USER_ID);

        assertThat(scope.memberOrganizationIds()).containsExactlyInAnyOrder(2L, 3L);
        assertThat(scope.ancestorOrSelfOrganizationIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
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

    private static UserOrganizationMembership membership(Organization organization) {
        UserOrganizationMembership membership = new UserOrganizationMembership();
        membership.setOrganization(organization);
        return membership;
    }
}
