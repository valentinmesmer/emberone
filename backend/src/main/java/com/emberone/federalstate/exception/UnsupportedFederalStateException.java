/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.exception;

import com.emberone.federalstate.domain.FederalState;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedFederalStateException extends RuntimeException {

    public UnsupportedFederalStateException(FederalState federalState) {
        super("Federal state is not configured: " + federalState);
    }
}
