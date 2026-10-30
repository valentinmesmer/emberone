/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.support;

import java.util.function.IntSupplier;
import lombok.experimental.UtilityClass;

/**
 * Bounded native {@code DELETE} batches for retention handlers (see repository {@code LIMIT 5000}).
 */
@UtilityClass
public class RetentionBatchDeletes {

    public static final int BATCH_SIZE = 5000;

    public static int deleteUntilExhausted(IntSupplier deleteOneBatch) {
        int total = 0;
        int deleted;
        while ((deleted = deleteOneBatch.getAsInt()) > 0) {
            total += deleted;
        }
        return total;
    }
}
