/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import com.emberone.retention.profile.RetentionProfile;

/**
 * Bundled federal-state baseline: tenant catalog plus org-setup templates for the SYSTEM standard.
 */
public record FederalStateBaselines(
        TenantBaseline tenant,
        OrganizationSetupBaseline organizationSetup,
        RetentionProfile retentionProfile) {}
