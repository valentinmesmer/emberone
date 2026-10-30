/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class WorkflowParticipantNotFoundException extends ResourceNotFoundException {
    public WorkflowParticipantNotFoundException(Long participantId) {
        super("Workflow participant not found: " + participantId);
    }
}
