/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import jakarta.persistence.*;
import lombok.*;

/**
 * Standard-library template mapping an organization group to a permission group, by {@code
 * config_key}. Materialized into per-org {@code perm_group_mappings} rows (e.g. LEADERSHIP &rarr;
 * Admin, ALL_MEMBERS &rarr; Standard).
 */
@Entity
@Table(
        name = "std_org_group_permission_group_templates",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {
                        "tenant_id",
                        "standard_id",
                        "org_group_config_key",
                        "permission_group_config_key"
                    })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrgGroupPermissionGroupTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "org_group_config_key", nullable = false)
    private String orgGroupConfigKey;

    @Column(name = "permission_group_config_key", nullable = false)
    private String permissionGroupConfigKey;
}
