/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CoursePrerequisiteKind;
import com.emberone.courseoffering.dto.CoursePrerequisiteCheckDto;
import com.emberone.qualification.service.PrerequisiteEvaluationService;
import com.emberone.qualification.service.PrerequisiteEvaluationService.Holdings;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checks a user against the prerequisites of an offering, using the shared evaluation logic. */
@Service
@RequiredArgsConstructor
public class CoursePrerequisiteService {

    private final PrerequisiteEvaluationService evaluationService;

    @Transactional(readOnly = true)
    public CoursePrerequisiteCheckDto check(CourseOffering offering, Long userId) {
        Holdings holdings = evaluationService.loadHoldings(userId);
        List<String> trainingKeys =
                offering.prerequisiteKeys(CoursePrerequisiteKind.TRAINING_COURSE);
        List<String> certKeys = offering.prerequisiteKeys(CoursePrerequisiteKind.CERTIFICATION);
        List<String> licenseKeys =
                offering.prerequisiteKeys(CoursePrerequisiteKind.DRIVING_LICENSE);
        boolean met = isMet(offering, holdings);
        return CoursePrerequisiteCheckDto.builder()
                .met(met)
                .trainingCourseRequirementMode(offering.getTrainingCourseRequirementMode())
                .certRequirementMode(offering.getCertRequirementMode())
                .drivingLicenseRequirementMode(offering.getDrivingLicenseRequirementMode())
                .trainingCourseDetails(
                        evaluationService.trainingCourseDetails(trainingKeys, holdings))
                .certDefinitionDetails(evaluationService.certDefinitionDetails(certKeys, holdings))
                .drivingLicenseDetails(
                        evaluationService.drivingLicenseDetails(licenseKeys, holdings))
                .build();
    }

    /** Offerings among {@code offerings} whose prerequisites the user does not meet. */
    @Transactional(readOnly = true)
    public Set<Long> unmetOfferingIds(Collection<CourseOffering> offerings, Long userId) {
        if (offerings.isEmpty()) {
            return Set.of();
        }
        Holdings holdings = evaluationService.loadHoldings(userId);
        return offerings.stream()
                .filter(offering -> !isMet(offering, holdings))
                .map(CourseOffering::getId)
                .collect(Collectors.toSet());
    }

    private boolean isMet(CourseOffering offering, Holdings holdings) {
        List<String> trainingKeys =
                offering.prerequisiteKeys(CoursePrerequisiteKind.TRAINING_COURSE);
        List<String> certKeys = offering.prerequisiteKeys(CoursePrerequisiteKind.CERTIFICATION);
        List<String> licenseKeys =
                offering.prerequisiteKeys(CoursePrerequisiteKind.DRIVING_LICENSE);
        return evaluationService.trainingCoursesMet(
                        trainingKeys, offering.getTrainingCourseRequirementMode(), holdings)
                && evaluationService.certDefinitionsMet(
                        certKeys, offering.getCertRequirementMode(), holdings)
                && evaluationService.drivingLicensesMet(
                        licenseKeys, offering.getDrivingLicenseRequirementMode(), holdings);
    }
}
