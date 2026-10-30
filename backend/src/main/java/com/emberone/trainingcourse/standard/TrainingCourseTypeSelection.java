/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.standard;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "std_training_course_type_selections",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "definition_config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingCourseTypeSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "definition_config_key", nullable = false)
    private String definitionConfigKey;
}
