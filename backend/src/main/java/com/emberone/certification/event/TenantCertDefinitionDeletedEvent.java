/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

/**
 * Published when a tenant certification definition is hard-deleted, so other modules (e.g.
 * qualification prerequisite links) can clean up without the catalog service depending on them.
 */
public record TenantCertDefinitionDeletedEvent(Long tenantId, String configKey) {}
