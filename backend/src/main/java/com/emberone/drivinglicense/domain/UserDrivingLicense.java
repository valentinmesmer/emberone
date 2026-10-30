/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.domain;

import com.emberone.platform.navigation.domain.AppLinkType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/**
 * A user's driving license document (one per user). {@code appLinkType=null} means manual entry;
 * {@code WORKFLOW} means approved workflow origin.
 */
@Entity
@Table(name = "driving_licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserDrivingLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "license_number", length = 128)
    private String licenseNumber;

    @Column(name = "issuing_authority", length = 512)
    private String issuingAuthority;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "document_expiration_date")
    private LocalDate documentExpirationDate;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "license", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<UserDrivingLicenseClass> classes = new ArrayList<>();

    @OneToMany(mappedBy = "license", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<UserDrivingLicenseDocument> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "app_link_type")
    private AppLinkType appLinkType;

    @Column(name = "app_link_id")
    private Long appLinkId;
}
