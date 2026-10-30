/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Database access settings for the {@code emberone.db} namespace. */
@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.db")
public class EmberOneDbProperties {

    /**
     * When {@code true}, SQL executed outside a transaction fails fast instead of only logging a
     * warning. Intended for development and tests, see {@code
     * TransactionRequiredStatementInspector}.
     */
    private boolean requireTransaction = false;
}
