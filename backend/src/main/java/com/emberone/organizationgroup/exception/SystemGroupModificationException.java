/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organizationgroup.exception;

import com.emberone.platform.exception.AccessDeniedException;

public class SystemGroupModificationException extends AccessDeniedException {
    public SystemGroupModificationException(String message) {
        super(message);
    }
}
