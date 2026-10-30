/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.navigation.domain.AppLinkType;
import org.junit.jupiter.api.Test;

class AppNavigationResolverTest {

    private final AppNavigationResolver resolver = new AppNavigationResolver();

    @Test
    void resolvesWorkflowPath() {
        assertThat(resolver.resolveNavigationPath(AppLinkType.WORKFLOW, 42L))
                .isEqualTo("/my-area/workflows/42");
    }

    @Test
    void resolvesCertificationDefinitionPath() {
        assertThat(resolver.resolveNavigationPath(AppLinkType.CERTIFICATION_RECORD, 15L))
                .isEqualTo("/my-area/certifications/15");
    }

    @Test
    void resolvesTrainingCourseRecordPath() {
        assertThat(resolver.resolveNavigationPath(AppLinkType.TRAINING_COURSE_RECORD, 22L))
                .isEqualTo("/my-area/training-courses/22");
    }

    @Test
    void resolvesDrivingLicenseSectionPath() {
        assertThat(resolver.resolveNavigationPath(AppLinkType.DRIVING_LICENSE_RECORD, 99L))
                .isEqualTo("/my-area/driving-licenses");
    }

    @Test
    void returnsNullWhenIncomplete() {
        assertThat(resolver.resolveNavigationPath(null, 1L)).isNull();
        assertThat(resolver.resolveNavigationPath(AppLinkType.WORKFLOW, null)).isNull();
    }

    @Test
    void certificationEventGrantNeedsModuleLookup() {
        assertThat(resolver.resolveNavigationPath(AppLinkType.CERTIFICATION_EVENT_GRANT, 9L))
                .isNull();
    }
}
