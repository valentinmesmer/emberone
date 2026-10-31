/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.navigation.domain;

/** Entity or row that stores an optional {@link AppLink}. */
public interface HasAppLink {

    AppLinkType getLinkType();

    Long getLinkId();
}
