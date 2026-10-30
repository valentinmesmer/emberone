/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.standard;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "std_rank_selections",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "rank_config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RankSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "rank_config_key", nullable = false)
    private String rankConfigKey;
}
