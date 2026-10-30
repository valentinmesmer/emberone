/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.event;

/**
 * Published while an organization group is still present, immediately before hard-delete or before
 * mappings are cleared on archive. Listeners run synchronously in the same transaction so
 * cross-package cleanup rolls back together with the group change.
 */
public record OrganizationGroupDeletingEvent(Long organizationGroupId, Long tenantId) {}
