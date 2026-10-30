/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.domain;

import jakarta.persistence.*;
import lombok.*;

/** One audience entry; {@code refId} is an organization or organization type id, per kind. */
@Entity
@Table(name = "course_offering_audiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOfferingAudience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private CourseAudienceKind kind;

    @Column(name = "ref_id")
    private Long refId;

    /**
     * Seats reserved for this entry out of the offering's maximum; null when the offering has no
     * quotas. Either all entries of an offering have one or none.
     */
    @Column(name = "quota")
    private Integer quota;
}
