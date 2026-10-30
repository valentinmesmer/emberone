/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/** A course offered by an organization; grants one tenant training course type on completion. */
@Entity
@Table(name = "course_offerings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "tenant_training_course_type_id", nullable = false)
    private Long trainingCourseTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_training_course_type_id", insertable = false, updatable = false)
    private TenantTrainingCourseType trainingCourseType;

    /** Optional description of what the offering covers (not the catalog type name). */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Set exactly while the status is {@code TEMPLATE}. */
    @Column(name = "template_name")
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CourseOfferingStatus status;

    @Column(name = "registration_start")
    private Instant registrationStart;

    @Column(name = "registration_end")
    private Instant registrationEnd;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "has_exam", nullable = false)
    private boolean hasExam;

    @Column(name = "requires_home_org_approval", nullable = false)
    private boolean requiresHomeOrgApproval;

    /** Cached from the sessions by {@link #syncSchedule()} so lists can sort and filter on it. */
    @Column(name = "first_session_start")
    private Instant firstSessionStart;

    @Column(name = "last_session_end")
    private Instant lastSessionEnd;

    /** Set exactly while the status is {@code COMPLETED}. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Set exactly while the status is {@code CANCELLED}, together with {@link #cancelReason}. */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Why the organization cancelled the offering; sent to everyone concerned. */
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /**
     * When the reminder before the course start went out; makes the sweep idempotent. Reset when
     * the first session moves.
     */
    @Column(name = "start_reminder_sent_at")
    private Instant startReminderSentAt;

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("start ASC, id ASC")
    @BatchSize(size = 32)
    @Builder.Default
    private List<CourseSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<CourseOfferingAudience> audiences = new ArrayList<>();

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

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<CourseOfferingPrerequisite> prerequisites = new ArrayList<>();

    /** Registration window contains {@code now} (start inclusive, end exclusive). */
    public boolean isRegistrationOpen(Instant now) {
        return registrationStart != null
                && registrationEnd != null
                && !now.isBefore(registrationStart)
                && now.isBefore(registrationEnd);
    }

    public List<String> prerequisiteKeys(CoursePrerequisiteKind kind) {
        return prerequisites.stream()
                .filter(prerequisite -> prerequisite.getKind() == kind)
                .map(CourseOfferingPrerequisite::getConfigKey)
                .toList();
    }

    /**
     * Recomputes the cached schedule; call after every change to {@link #sessions}. A moved first
     * session re-arms the start reminder.
     */
    public void syncSchedule() {
        Instant previousFirstSessionStart = firstSessionStart;
        firstSessionStart =
                sessions.stream()
                        .map(CourseSession::getStart)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
        lastSessionEnd =
                sessions.stream()
                        .map(CourseSession::getEnd)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
        if (!Objects.equals(previousFirstSessionStart, firstSessionStart)) {
            startReminderSentAt = null;
        }
    }
}
