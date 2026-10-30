/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroupmapping.domain;

import com.emberone.permissiongroup.domain.PermissionGroup;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "perm_group_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {"tenant_id", "organization_group_id", "permission_group_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionGroupMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_group_id", nullable = false)
    private Long organizationGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_group_id", nullable = false)
    private PermissionGroup permissionGroup;

    /**
     * True when this mapping was materialized from a standard's mapping template (and is therefore
     * removed automatically when that template mapping is deleted). False for manually created
     * mappings, which are never auto-removed.
     */
    @Column(name = "standard_managed", nullable = false)
    @Builder.Default
    private boolean standardManaged = false;
}
