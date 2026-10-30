/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.domain;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.MembershipTimestampSupport;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "user_organization_group_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserOrganizationGroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_group_id", nullable = false)
    private OrganizationGroup organizationGroup;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    /** Set when end side effects were applied; null while an {@code endDate} is still pending. */
    @Column(name = "end_processed_at")
    private Instant endProcessedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    private GroupMembershipType membershipType;

    public boolean isActiveAt(Instant at) {
        return MembershipTimestampSupport.isActiveAt(startDate, endDate, at);
    }
}
