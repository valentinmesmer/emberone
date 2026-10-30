/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Opening context handed to a {@link WorkflowHandler}: who opens the workflow and the typed
 * payload.
 */
@Getter
@RequiredArgsConstructor
public class WorkflowOpenContext<P> {
    private final Long openedByUserId;
    private final Long tenantId;
    private final P payload;
}
