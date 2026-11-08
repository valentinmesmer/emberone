/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.extraction;

/**
 * SPI implemented by a proof domain (certification, training course, driving license). The domain
 * declares fields, allowed catalog values and plausibility rules; {@link DocumentExtractionService}
 * does everything else. Implementations load catalogs only through the domain's existing services
 * in the caller's security and tenant context (no repositories of their own, no tenant override).
 */
public interface DocumentExtractionSchema {

    /** Stable key, equal to the workflow type name, e.g. {@code CERTIFICATION_RECORD}. */
    String type();

    /**
     * Prepares an extraction for the current user.
     *
     * @param userId the calling user
     * @param selectionId the catalog entry the user already chose in the form, if the type needs
     *     one (certification); otherwise null
     */
    PreparedExtraction prepare(long userId, Long selectionId);
}
