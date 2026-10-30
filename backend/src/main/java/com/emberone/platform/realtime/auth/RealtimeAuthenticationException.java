/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

public class RealtimeAuthenticationException extends RuntimeException {

    public RealtimeAuthenticationException(String message) {
        super(message);
    }

    public RealtimeAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
