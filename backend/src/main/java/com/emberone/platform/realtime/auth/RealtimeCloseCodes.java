/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RealtimeCloseCodes {

    /**
     * Custom WebSocket close code: JWT used for the STOMP session has expired. Clients should
     * reconnect with a fresh token.
     */
    public static final int TOKEN_EXPIRED = 4001;

    public static final String TOKEN_EXPIRED_REASON = "access_token_expired";
}
