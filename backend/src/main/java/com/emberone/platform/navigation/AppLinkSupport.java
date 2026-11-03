/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.navigation;

import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.platform.navigation.domain.MutableHasAppLink;
import lombok.experimental.UtilityClass;

/** Helpers for persisting {@link AppLink} across modules. */
@UtilityClass
public class AppLinkSupport {

    public static void applyTo(MutableHasAppLink target, AppLink link) {
        if (target == null) {
            return;
        }
        if (link == null) {
            target.setLinkType(null);
            target.setLinkId(null);
            return;
        }
        target.setLinkType(link.type());
        target.setLinkId(link.linkId());
    }
}
