/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.navigation.domain;

/** Entity or row that stores an optional {@link AppLink}. */
public interface HasAppLink {

    AppLinkType getLinkType();

    Long getLinkId();
}
