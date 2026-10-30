/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * One entry of a managing organization's audience. {@code refId} is an organization or organization
 * type id, per kind. {@code permissionTypes} is the allowed set of {@code PermissionType} names,
 * comma separated; an activation may only pick from it.
 */
@Entity
@Table(name = "org_management_audiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementAudience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "managing_organization_id", nullable = false)
    private Long managingOrganizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private ManagementAudienceKind kind;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "permission_types", nullable = false, length = 4096)
    private String permissionTypes;

    /** Upper bound for one activation that uses this entry. */
    @Column(name = "max_duration_minutes", nullable = false)
    private Integer maxDurationMinutes;
}
