/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.navigation;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.navigation.domain.HasAppLink;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Resolves relative in-app routes from semantic link targets. Single place for URL structure - must
 * stay aligned with {@code frontend/app/}.
 */
@Component
@Named("AppNavigationResolver")
public class AppNavigationResolver {

    private static final String MY_AREA_BASE = "/my-area";

    public String resolveNavigationPath(AppLinkType linkType, Long linkId) {
        if (linkType == null || linkId == null) {
            return null;
        }
        return switch (linkType) {
            case WORKFLOW -> MY_AREA_BASE + "/workflows/" + linkId;
            case CERTIFICATION_RECORD -> MY_AREA_BASE + "/certifications/" + linkId;
            case TRAINING_COURSE_RECORD -> MY_AREA_BASE + "/training-courses/" + linkId;
            case DRIVING_LICENSE_RECORD -> MY_AREA_BASE + "/driving-licenses";
            case ONBOARDING_INVITATION -> MY_AREA_BASE + "/onboarding/" + linkId;
            // Needs grant→event lookup - see CertificationEventGrantNavigationResolver.
            case CERTIFICATION_EVENT_GRANT -> null;
            case COURSE_REGISTRATION -> null;
        };
    }

    @Named("navigationPath")
    public String navigationPath(HasAppLink source) {
        if (source == null) {
            return null;
        }
        return resolveNavigationPath(source.getLinkType(), source.getLinkId());
    }
}
