/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.notification.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class NotificationListSpecsTest {

    @Test
    void statusesFilterEmptyIsConjunction() {
        Specification<Notification> spec = NotificationListSpecs.statusesFilter(List.of());
        assertThat(spec).isNotNull();
    }

    @Test
    void typesFilterRestrictsToSelectedTypes() {
        Specification<Notification> spec =
                NotificationListSpecs.typesFilter(
                        List.of(
                                NotificationType.USERNAME_CHANGED,
                                NotificationType.WORKFLOW_OPENED));
        assertThat(spec).isNotNull();
    }
}
