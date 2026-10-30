/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.standard;

import com.emberone.organizationgroup.domain.OrganizationGroupType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Standard-library template for an organization group. Belongs to a {@code std_standards} row and
 * is keyed by {@code config_key}. Materialized into a per-org {@code organization_groups} row. For
 * system groups {@code name} is null and resolved (localized) at materialization time.
 */
@Entity
@Table(
        name = "std_org_group_templates",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrgGroupTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    /**
     * Explicit name for custom templates; null for system groups (localized name resolved on
     * materialize).
     */
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private OrganizationGroupType groupType;
}
