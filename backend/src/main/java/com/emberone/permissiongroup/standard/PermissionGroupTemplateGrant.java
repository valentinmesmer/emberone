/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import com.emberone.permission.domain.PermissionType;
import jakarta.persistence.*;
import lombok.*;

/** A {@link PermissionType} grant of a {@link PermissionGroupTemplate}. */
@Entity
@Table(name = "std_permission_group_template_grants")
@IdClass(PermissionGroupTemplateGrantPK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupTemplateGrant {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private PermissionGroupTemplate template;

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
