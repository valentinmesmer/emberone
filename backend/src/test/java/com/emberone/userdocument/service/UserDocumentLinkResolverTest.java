/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.userdocument.domain.UserDocumentLink;
import org.junit.jupiter.api.Test;

class UserDocumentLinkResolverTest {

    private final UserDocumentLinkResolver resolver =
            new UserDocumentLinkResolver(new AppNavigationResolver());

    @Test
    void resolvesWorkflowNavigationPath() {
        var dto = resolver.toDto(link(2L, AppLinkType.WORKFLOW, 5L));

        assertThat(dto.getLinkType()).isEqualTo(AppLinkType.WORKFLOW);
        assertThat(dto.getLinkId()).isEqualTo(5L);
        assertThat(dto.getNavigationPath()).isEqualTo("/my-area/workflows/5");
    }

    @Test
    void resolvesCertificationRecordNavigationPath() {
        var dto = resolver.toDto(link(3L, AppLinkType.CERTIFICATION_RECORD, 15L));

        assertThat(dto.getLinkType()).isEqualTo(AppLinkType.CERTIFICATION_RECORD);
        assertThat(dto.getLinkId()).isEqualTo(15L);
        assertThat(dto.getNavigationPath()).isEqualTo("/my-area/certifications/15");
    }

    @Test
    void resolvesTrainingCourseRecordNavigationPath() {
        var dto = resolver.toDto(link(4L, AppLinkType.TRAINING_COURSE_RECORD, 22L));

        assertThat(dto.getLinkType()).isEqualTo(AppLinkType.TRAINING_COURSE_RECORD);
        assertThat(dto.getLinkId()).isEqualTo(22L);
        assertThat(dto.getNavigationPath()).isEqualTo("/my-area/training-courses/22");
    }

    private static UserDocumentLink link(Long id, AppLinkType type, Long linkId) {
        return UserDocumentLink.builder().id(id).linkType(type).linkId(linkId).build();
    }
}
