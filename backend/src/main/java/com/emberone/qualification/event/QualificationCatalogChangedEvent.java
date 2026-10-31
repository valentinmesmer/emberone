/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.event;

import java.util.List;

public record QualificationCatalogChangedEvent(Long tenantId, List<Long> organizationIds) {}
