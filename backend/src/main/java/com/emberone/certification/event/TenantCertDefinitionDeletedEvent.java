/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.event;

/**
 * Published when a tenant certification definition is hard-deleted, so other modules (e.g.
 * qualification prerequisite links) can clean up without the catalog service depending on them.
 */
public record TenantCertDefinitionDeletedEvent(Long tenantId, String configKey) {}
