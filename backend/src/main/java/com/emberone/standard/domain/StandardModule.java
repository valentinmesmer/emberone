/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.domain;

import com.emberone.module.domain.Module;
import jakarta.persistence.*;
import lombok.*;

/**
 * A toggleable {@link Module} disabled for a {@link Standard}. Missing row = module enabled. {@link
 * Module#CORE} is never stored.
 */
@Entity
@Table(name = "std_standard_modules")
@IdClass(StandardModulePK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardModule {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Id
    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false)
    private Module module;
}
