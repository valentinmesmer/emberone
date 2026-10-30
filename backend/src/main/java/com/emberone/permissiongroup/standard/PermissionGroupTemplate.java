/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * Standard-library template for a permission group. Belongs to a {@code std_standards} row, keyed
 * by {@code config_key}, and grants {@link PermissionType}s directly. Materialized into a per-org
 * {@code perm_groups} row plus its grant rows.
 */
@Entity
@Table(
        name = "std_permission_group_templates",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PermissionGroupTemplate {

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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private PermissionGroupType groupType;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PermissionGroupTemplateGrant> grants = new ArrayList<>();

    public List<PermissionType> getPermissionTypes() {
        if (grants == null || grants.isEmpty()) {
            return List.of();
        }
        return grants.stream().map(PermissionGroupTemplateGrant::getPermissionType).toList();
    }

    public void setPermissionTypes(List<PermissionType> permissionTypes) {
        if (this.grants == null) {
            this.grants = new ArrayList<>();
        }
        this.grants.clear();
        if (permissionTypes == null) {
            return;
        }
        permissionTypes.forEach(this::addPermissionType);
    }

    public void addPermissionType(PermissionType permissionType) {
        boolean alreadyGranted =
                this.grants.stream().anyMatch(grant -> grant.getPermissionType() == permissionType);
        if (alreadyGranted) {
            return;
        }
        PermissionGroupTemplateGrant grant = new PermissionGroupTemplateGrant();
        grant.setTenantId(this.tenantId);
        grant.setTemplate(this);
        grant.setPermissionType(permissionType);
        this.grants.add(grant);
    }
}
