/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.domain;

import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * A user of a managing organization opens a target organization until {@code validUntil}. {@code
 * permissionTypes} is a comma-separated list of {@code PermissionType} names, the chosen scope at
 * activation time. {@code userId} becomes null when the user is deleted.
 */
@Entity
@Table(name = "org_management_activations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "managing_organization_id", nullable = false)
    private Long managingOrganizationId;

    @Column(name = "target_organization_id", nullable = false)
    private Long targetOrganizationId;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ended_by")
    private ManagementActivationEndedBy endedBy;

    @Column(name = "permission_types", nullable = false, length = 4096)
    private String permissionTypes;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /** Set once the expiry event for this activation was published. Null until then. */
    @Column(name = "expiry_notified_at")
    private Instant expiryNotifiedAt;

    // Read-only views of the ids above, for sorting and searching the activation log by name.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managing_organization_id", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Organization managingOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Organization targetOrganization;

    public boolean isOpen() {
        return endedAt == null;
    }
}
