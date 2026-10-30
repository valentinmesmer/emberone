/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.domain;

import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(
        name = "perm_groups",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "organization_id", "name"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PermissionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PermissionGroupType type;

    /**
     * Stable key linking this instance row to the standard template it was materialized from; null
     * for ad-hoc org-only groups.
     */
    @Column(name = "config_key")
    private String configKey;

    /**
     * Id of the {@code std_permission_group_templates} item this row was materialized from; null
     * when the row is no longer tied to a standard template.
     */
    @Column(name = "standard_origin_id")
    private Long standardOriginId;

    @OneToMany(mappedBy = "permissionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PermissionGroupPermissionType> permissionTypeLinks = new ArrayList<>();

    public List<PermissionType> getPermissionTypes() {
        if (permissionTypeLinks == null || permissionTypeLinks.isEmpty()) {
            return List.of();
        }
        return permissionTypeLinks.stream()
                .map(PermissionGroupPermissionType::getPermissionType)
                .toList();
    }

    public void setPermissionTypes(List<PermissionType> permissionTypes) {
        if (this.permissionTypeLinks == null) {
            this.permissionTypeLinks = new ArrayList<>();
        }
        this.permissionTypeLinks.clear();
        if (permissionTypes == null) {
            return;
        }
        for (PermissionType permissionType : permissionTypes) {
            addPermissionType(permissionType);
        }
    }

    public void addPermissionType(PermissionType permissionType) {
        boolean alreadyGranted =
                this.permissionTypeLinks.stream()
                        .anyMatch(link -> link.getPermissionType() == permissionType);
        if (alreadyGranted) {
            return;
        }
        PermissionGroupPermissionType link = new PermissionGroupPermissionType();
        link.setTenantId(this.tenantId);
        link.setPermissionGroup(this);
        link.setPermissionType(permissionType);
        this.permissionTypeLinks.add(link);
    }
}
