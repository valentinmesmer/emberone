/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class EventNotFoundException extends ResourceNotFoundException {

    public EventNotFoundException(String message) {
        super(message);
    }
}
