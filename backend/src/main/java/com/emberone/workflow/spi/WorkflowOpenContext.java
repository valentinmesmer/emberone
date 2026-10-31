/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
