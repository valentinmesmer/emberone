/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "training_course_user_submission_documents",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_training_course_user_submission_documents_unique",
                        columnNames = {"tenant_id", "submission_id", "document_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TrainingCourseUserSubmissionDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private TrainingCourseUserSubmission submission;

    @Column(name = "document_id", nullable = false)
    private Long documentId;
}
