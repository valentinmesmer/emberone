/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FederalStateTest {

    @Test
    void fromValue_parsesEnumName() {
        assertThat(FederalState.fromValue("BAYERN")).isEqualTo(FederalState.BAYERN);
        assertThat(FederalState.fromValue("nordrhein_westfalen"))
                .isEqualTo(FederalState.NORDRHEIN_WESTFALEN);
    }

    @Test
    void fromValue_defaultsToBayernWhenBlank() {
        assertThat(FederalState.fromValue(null)).isEqualTo(FederalState.BAYERN);
        assertThat(FederalState.fromValue("  ")).isEqualTo(FederalState.BAYERN);
    }

    @Test
    void fromValue_rejectsUnknown() {
        assertThatThrownBy(() -> FederalState.fromValue("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported federal state");
    }

    @Test
    void toValue_returnsEnumName() {
        assertThat(FederalState.BAYERN.toValue()).isEqualTo("BAYERN");
    }
}
