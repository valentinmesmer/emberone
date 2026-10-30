/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StandardConfigKeyTest {

    @Test
    void allocateCustomConfigKey_transliteratesGermanUmlautsInSlug() {
        assertThat(StandardConfigKey.allocateCustomConfigKey("Büro", s -> false))
                .isEqualTo("custom:buero");
        assertThat(StandardConfigKey.allocateCustomConfigKey("Führungskraft", s -> false))
                .isEqualTo("custom:fuehrungskraft");
        assertThat(StandardConfigKey.allocateCustomConfigKey("Straße", s -> false))
                .isEqualTo("custom:strasse");
    }

    @Test
    void customFromMessageKey_usesLastSegmentAsSlug() {
        assertThat(StandardConfigKey.customFromMessageKey("organizationGroup.custom.office"))
                .isEqualTo("custom:office");
        assertThat(StandardConfigKey.customFromMessageKey("permissionGroup.custom.leadershipTeam"))
                .isEqualTo("custom:leadershipTeam");
    }

    @Test
    void allocateCustomConfigKey_appendsSuffixOnCollision() {
        Set<String> taken = new HashSet<>();
        taken.add("custom:buero");

        String key = StandardConfigKey.allocateCustomConfigKey("Büro", taken::contains);

        assertThat(key).isEqualTo("custom:buero-2");
    }

    @Test
    void allocateCustomConfigKey_treatsBueroAndBüroAsSameBase() {
        Set<String> taken = new HashSet<>();
        taken.add("custom:buero");

        assertThat(StandardConfigKey.allocateCustomConfigKey("Buero", taken::contains))
                .isEqualTo("custom:buero-2");
    }

    @Test
    void allocateCustomConfigKey_producesUrlSafeAsciiSlug() {
        assertThat(StandardConfigKey.allocateCustomConfigKey("Team A / B", s -> false))
                .isEqualTo("custom:team-a-b");
    }
}
