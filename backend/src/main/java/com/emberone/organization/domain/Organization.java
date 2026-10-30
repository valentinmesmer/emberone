/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_code", nullable = false)
    private String organizationCode;

    @Column(name = "name", nullable = false)
    private String name;

    /** The tenant's {@link OrganizationTypeDefinition} this organization belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_type_id", nullable = false)
    private OrganizationTypeDefinition type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_organization_id")
    private Organization parentOrganization;

    /**
     * Explicit per-org standard override. When {@code null} the organization inherits the standard
     * assigned to its {@link #type} ({@code std_assignments}); when set, this standard takes
     * precedence instead of inheriting the standard assigned to its org type.
     */
    @Column(name = "standard_id")
    private Long standardId;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    public boolean isDeactivated() {
        return deactivatedAt != null;
    }
}
