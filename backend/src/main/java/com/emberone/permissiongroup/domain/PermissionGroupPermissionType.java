/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.domain;

import com.emberone.permission.domain.PermissionType;
import jakarta.persistence.*;
import lombok.*;

/**
 * A grant linking a {@link PermissionGroup} to a {@link PermissionType} directly. Replaces the
 * former Permission groups grant permission types directly (no separate permissions table).
 */
@Entity
@Table(name = "perm_group_permission_types")
@IdClass(PermissionGroupPermissionTypePK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupPermissionType {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_group_id", nullable = false)
    private PermissionGroup permissionGroup;

    @Id
    @Column(name = "permission_type_id", nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Long permissionType;

    public PermissionType getPermissionType() {
        return permissionType == null ? null : PermissionType.fromId(permissionType);
    }

    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType == null ? null : permissionType.getId();
    }
}
