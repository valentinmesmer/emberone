/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Minimal organization node for admin hierarchy tree (read-only overview). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrganizationTreeNodeDto {
    private Long id;
    private String name;
    private String organizationCode;
    private Long organizationTypeId;

    /** True if this org has no active member in the leadership group. */
    private boolean leaderMissing;

    /** True if this org or any descendant has no active member in the leadership group. */
    private boolean leaderMissingInSubtree;

    private Instant deactivatedAt;

    @Builder.Default private List<AdminOrganizationTreeNodeDto> children = new ArrayList<>();
}
