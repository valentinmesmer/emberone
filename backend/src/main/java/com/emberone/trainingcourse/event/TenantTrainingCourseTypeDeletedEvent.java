/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.event;

/**
 * Published when a tenant training-course type is hard-deleted, so other modules (e.g.
 * qualification prerequisite links) can clean up without the catalog service depending on them.
 */
public record TenantTrainingCourseTypeDeletedEvent(Long tenantId, String configKey) {}
