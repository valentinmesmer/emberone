/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.exceptionhandler;

import java.io.IOException;
import lombok.experimental.UtilityClass;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@UtilityClass
class ClientDisconnectSupport {

    static boolean isClientDisconnect(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ClientAbortException
                    || current instanceof AsyncRequestNotUsableException) {
                return true;
            }
            if (current instanceof IOException ioEx && isBrokenConnection(ioEx)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBrokenConnection(IOException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("Broken pipe") || message.contains("Connection reset");
    }
}
