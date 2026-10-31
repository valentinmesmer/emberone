/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.spi;

/**
 * An additional approver organization contributed by a dynamic handler. All approver orgs are
 * mandatory.
 */
public record ApproverOrg(Long organizationId) {}
