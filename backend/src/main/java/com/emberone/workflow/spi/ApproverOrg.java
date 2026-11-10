/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.spi;

/**
 * An approver organization computed by a handler at runtime.
 *
 * @param slotRole one of {@link WorkflowHandler#approverSlotRoles()}, see {@link
 *     WorkflowApproverSlotRoles}
 */
public record ApproverOrg(Long organizationId, String slotRole) {}
