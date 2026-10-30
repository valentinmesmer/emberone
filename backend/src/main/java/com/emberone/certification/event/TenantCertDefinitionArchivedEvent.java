/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

/**
 * Published when a tenant certification definition is archived, so reminder to-dos for that
 * definition can be cancelled without the catalog service depending on {@code TodoService}.
 */
public record TenantCertDefinitionArchivedEvent(Long tenantId, Long tenantCertDefinitionId) {}
