/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
