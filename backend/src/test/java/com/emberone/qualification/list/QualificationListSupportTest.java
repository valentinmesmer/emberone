/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.qualification.domain.QualificationExerciseStatus;
import com.emberone.qualification.dto.QualificationStatusItemDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class QualificationListSupportTest {

    @Test
    void statusFilterNarrowsItems() {
        QualificationStatusItemDto open =
                QualificationStatusItemDto.builder()
                        .tenantQualificationDefinitionId(1L)
                        .name("Alpha")
                        .abbreviation("A")
                        .configKey("a")
                        .status(QualificationExerciseStatus.CAN_EXERCISE)
                        .build();
        QualificationStatusItemDto denied =
                QualificationStatusItemDto.builder()
                        .tenantQualificationDefinitionId(2L)
                        .name("Beta")
                        .abbreviation("B")
                        .configKey("b")
                        .status(QualificationExerciseStatus.ORG_DECISION_DENIED)
                        .build();

        List<QualificationStatusItemDto> result =
                QualificationListSupport.filterAndSort(
                        List.of(open, denied),
                        new ListQuery(0, 50, null, "name,asc"),
                        new QualificationListFilter(QualificationExerciseStatus.CAN_EXERCISE));

        assertThat(result).containsExactly(open);
    }
}
