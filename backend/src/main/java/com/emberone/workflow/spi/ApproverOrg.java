/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

/**
 * An additional approver organization contributed by a dynamic handler. All approver orgs are
 * mandatory.
 */
public record ApproverOrg(Long organizationId) {}
