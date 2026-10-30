/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "std_function_grants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FunctionGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "organization_group_config_key", nullable = false)
    private String organizationGroupConfigKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    private GroupMembershipType membershipType;

    @Column(name = "function_config_key", nullable = false)
    private String functionConfigKey;
}
