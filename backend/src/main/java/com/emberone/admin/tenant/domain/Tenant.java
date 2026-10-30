/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.domain;

import com.emberone.federalstate.domain.FederalState;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "abbreviation", nullable = false, length = 32, unique = true)
    private String abbreviation;

    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private SupportedLocale locale = SupportedLocale.DE;

    @Enumerated(EnumType.STRING)
    @Column(name = "federal_state", nullable = false, length = 32)
    @Builder.Default
    private FederalState federalState = FederalState.BAYERN;
}
