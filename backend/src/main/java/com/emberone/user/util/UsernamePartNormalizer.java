/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.util;

public final class UsernamePartNormalizer {

    private UsernamePartNormalizer() {}

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9]", "");
    }
}
