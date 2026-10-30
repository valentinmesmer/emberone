/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.navigation.domain;

/** JPA entities that persist an optional {@link AppLink}. */
public interface MutableHasAppLink extends HasAppLink {

    void setLinkType(AppLinkType linkType);

    void setLinkId(Long linkId);
}
