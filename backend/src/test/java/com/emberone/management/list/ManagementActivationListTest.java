/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.domain.ManagementActivationEndedBy;
import com.emberone.management.domain.ManagementActivationStatus;
import com.emberone.platform.web.list.ListQuery;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ManagementActivationListTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void statusIsDerivedFromEndAndValidity() {
        assertThat(ManagementActivationStatus.of(activation(NOW.plusSeconds(60), null), NOW))
                .isEqualTo(ManagementActivationStatus.ACTIVE);
        assertThat(ManagementActivationStatus.of(activation(NOW, null), NOW))
                .isEqualTo(ManagementActivationStatus.EXPIRED);
        assertThat(ManagementActivationStatus.of(activation(NOW.plusSeconds(60), NOW), NOW))
                .isEqualTo(ManagementActivationStatus.ENDED);
        assertThat(ManagementActivationStatus.of(activation(NOW.minusSeconds(60), NOW), NOW))
                .isEqualTo(ManagementActivationStatus.ENDED);
    }

    @Test
    void filterParsesStatusAndIds() {
        ManagementActivationListFilter filter =
                ManagementActivationListFilter.parse(
                        List.of(9L), List.of("ACTIVE", "ENDED"), List.of(1L), List.of(2L, 3L));

        assertThat(filter.statuses())
                .containsExactly(
                        ManagementActivationStatus.ACTIVE, ManagementActivationStatus.ENDED);
        assertThat(filter.tenantIds()).containsExactly(9L);
        assertThat(filter.organizationIds()).containsExactly(1L);
        assertThat(filter.managingOrganizationIds()).containsExactly(2L, 3L);
    }

    @Test
    void emptyFilterMeansNoRestriction() {
        ManagementActivationListFilter filter =
                ManagementActivationListFilter.parse(null, null, null, null);

        assertThat(filter.tenantIds()).isEmpty();
        assertThat(filter.statuses()).isEmpty();
        assertThat(filter.organizationIds()).isEmpty();
        assertThat(filter.managingOrganizationIds()).isEmpty();
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(
                        () ->
                                ManagementActivationListFilter.parse(
                                        null, List.of("NOPE"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultSortIsNewestFirst() {
        Pageable pageable =
                ManagementActivationListDefinition.INSTANCE.pageable(
                        new ListQuery(0, null, null, null));

        assertThat(pageable.getSort().getOrderFor("activatedAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("activatedAt").isDescending()).isTrue();
    }

    @Test
    void sortKeysMapToJoinedNames() {
        Pageable person =
                ManagementActivationListDefinition.INSTANCE.pageable(
                        new ListQuery(0, null, null, "person,asc"));
        Pageable target =
                ManagementActivationListDefinition.INSTANCE.pageable(
                        new ListQuery(0, null, null, "targetOrganization,desc"));

        assertThat(person.getSort().getOrderFor("user.lastName")).isNotNull();
        assertThat(target.getSort().getOrderFor("targetOrganization.name")).isNotNull();
        assertThatThrownBy(
                        () ->
                                ManagementActivationListDefinition.INSTANCE.pageable(
                                        new ListQuery(0, null, null, "reason,asc")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ManagementActivation activation(Instant validUntil, Instant endedAt) {
        return ManagementActivation.builder()
                .validUntil(validUntil)
                .endedAt(endedAt)
                .endedBy(endedAt == null ? null : ManagementActivationEndedBy.USER)
                .build();
    }
}
