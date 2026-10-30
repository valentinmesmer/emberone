/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import com.emberone.qualification.domain.QualificationRequirementMode;
import java.util.List;

public record QualificationDefinitionBaseline(
        String configKey,
        String name,
        String description,
        boolean requiresOrgDecision,
        QualificationRequirementMode trainingCourseRequirementMode,
        QualificationRequirementMode certRequirementMode,
        QualificationRequirementMode drivingLicenseRequirementMode,
        List<String> trainingCourseConfigKeys,
        List<String> certDefinitionConfigKeys,
        List<String> drivingLicenseDefinitionConfigKeys,
        boolean active) {

    public QualificationDefinitionBaseline {
        trainingCourseConfigKeys =
                trainingCourseConfigKeys == null
                        ? List.of()
                        : List.copyOf(trainingCourseConfigKeys);
        certDefinitionConfigKeys =
                certDefinitionConfigKeys == null
                        ? List.of()
                        : List.copyOf(certDefinitionConfigKeys);
        drivingLicenseDefinitionConfigKeys =
                drivingLicenseDefinitionConfigKeys == null
                        ? List.of()
                        : List.copyOf(drivingLicenseDefinitionConfigKeys);
    }
}
