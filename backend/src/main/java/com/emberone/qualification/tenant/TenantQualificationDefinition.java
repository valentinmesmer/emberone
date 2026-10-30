/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.tenant;

import com.emberone.platform.domain.LifecycleFields;
import com.emberone.qualification.domain.QualificationRequirementMode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(
        name = "tenant_qualification_definitions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "config_key"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TenantQualificationDefinition implements LifecycleFields.ActivatableCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "requires_org_decision", nullable = false)
    @Builder.Default
    private boolean requiresOrgDecision = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_course_requirement_mode", nullable = false)
    @Builder.Default
    private QualificationRequirementMode trainingCourseRequirementMode =
            QualificationRequirementMode.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_requirement_mode", nullable = false)
    @Builder.Default
    private QualificationRequirementMode certRequirementMode = QualificationRequirementMode.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "driving_license_requirement_mode", nullable = false)
    @Builder.Default
    private QualificationRequirementMode drivingLicenseRequirementMode =
            QualificationRequirementMode.ALL;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToMany(
            mappedBy = "qualificationDefinition",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<TenantQualificationTrainingCourse> trainingCourses = new ArrayList<>();

    @OneToMany(
            mappedBy = "qualificationDefinition",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<TenantQualificationCertDefinition> certDefinitions = new ArrayList<>();

    @OneToMany(
            mappedBy = "qualificationDefinition",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<TenantQualificationDrivingLicense> drivingLicenses = new ArrayList<>();
}
