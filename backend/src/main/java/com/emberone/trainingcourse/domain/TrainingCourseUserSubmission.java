/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.domain;

import com.emberone.platform.navigation.domain.AppLinkType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/**
 * A user's training course entry (one proof may grant multiple course types). {@code
 * appLinkType=null} means manual entry; {@code WORKFLOW} means approved workflow origin.
 */
@Entity
@Table(name = "training_course_user_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TrainingCourseUserSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "location", length = 512)
    private String location;

    @Column(name = "notes")
    private String notes;

    @Column(name = "hours", precision = 8, scale = 2)
    private BigDecimal hours;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<TrainingCourseUserSubmissionGrantedType> grantedTypes = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<TrainingCourseUserSubmissionDocument> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "app_link_type")
    private AppLinkType appLinkType;

    @Column(name = "app_link_id")
    private Long appLinkId;
}
