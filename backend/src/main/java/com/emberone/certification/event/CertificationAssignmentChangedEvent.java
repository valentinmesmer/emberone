/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

/**
 * Published when a user's certification assignments or records changed, so reminder to-dos for that
 * user should be re-synced against the current status.
 */
public record CertificationAssignmentChangedEvent(Long tenantId, Long userId) {}
