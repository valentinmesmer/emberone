/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.domain;

import com.emberone.organization.domain.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_absence_organizations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_user_absence_org",
                        columnNames = {"tenant_id", "user_absence_id", "organization_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserAbsenceOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_absence_id", nullable = false)
    private UserAbsence absence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
