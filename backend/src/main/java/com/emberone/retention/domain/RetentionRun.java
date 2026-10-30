/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "retention_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RetentionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private RetentionCategory category;

    @Column(name = "profile_key", nullable = false, length = 64)
    private String profileKey;

    @Column(name = "period_label", nullable = false, length = 32)
    private String periodLabel;

    @Column(name = "deleted_count", nullable = false)
    private int deletedCount;

    @Column(name = "ran_at", nullable = false)
    @Builder.Default
    private Instant ranAt = Instant.now();
}
