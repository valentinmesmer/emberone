/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "std_function_selections",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "function_config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FunctionSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "function_config_key", nullable = false)
    private String functionConfigKey;
}
