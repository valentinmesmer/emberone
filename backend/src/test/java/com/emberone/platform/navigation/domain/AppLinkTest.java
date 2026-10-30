/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.navigation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AppLinkTest {

    @Test
    void workflowBuildsSemanticLink() {
        AppLink link = AppLink.workflow(42);
        assertThat(link.type()).isEqualTo(AppLinkType.WORKFLOW);
        assertThat(link.linkId()).isEqualTo(42L);
    }

    @Test
    void rejectsMissingType() {
        assertThatThrownBy(() -> new AppLink(null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveId() {
        assertThatThrownBy(() -> AppLink.workflow(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
