/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
